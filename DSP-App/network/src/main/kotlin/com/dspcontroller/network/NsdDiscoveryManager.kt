package com.dspcontroller.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.dspcontroller.domain.model.DiscoveredDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages mDNS/NSD discovery for DSP devices on the local network.
 *
 * Uses Android's [NsdManager] to discover services of type `_dsp._tcp.`.
 * Discovered devices are exposed as a [StateFlow] and automatically pruned
 * if they haven't been re-advertised within [STALE_TIMEOUT_MS].
 *
 * Paper Reference: Zero-configuration networking for IoT device discovery.
 */
@Singleton
class NsdDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NsdDiscoveryManager"
        private const val SERVICE_TYPE = "_dsp._tcp."
        private const val STALE_TIMEOUT_MS = 30_000L
        private const val PRUNE_INTERVAL_MS = 10_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())

    /** Stream of currently discovered devices on the local network. */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var pruneJob: Job? = null
    private var isDiscovering = false

    // WHY: Track last-seen timestamps to prune stale entries that are no longer advertising.
    private val deviceTimestamps = mutableMapOf<String, Long>()

    /**
     * Begin mDNS discovery for `_dsp._tcp.` services.
     * Safe to call multiple times — subsequent calls are no-ops if already discovering.
     */
    fun startDiscovery() {
        if (isDiscovering) {
            Timber.tag(TAG).d("Discovery already running — ignoring start request")
            return
        }

        Timber.tag(TAG).i("Starting NSD discovery for %s", SERVICE_TYPE)

        val listener = object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(serviceType: String) {
                Timber.tag(TAG).i("Discovery started for %s", serviceType)
                isDiscovering = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.tag(TAG).d("Service found: %s", serviceInfo.serviceName)
                resolveService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.tag(TAG).d("Service lost: %s", serviceInfo.serviceName)
                removeDevice(serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.tag(TAG).i("Discovery stopped for %s", serviceType)
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag(TAG).e("Start discovery failed: error=%d", errorCode)
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.tag(TAG).e("Stop discovery failed: error=%d", errorCode)
            }
        }

        discoveryListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            startPruning()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start NSD discovery")
        }
    }

    /**
     * Stop mDNS discovery and clear all discovered devices.
     */
    fun stopDiscovery() {
        Timber.tag(TAG).i("Stopping NSD discovery")
        pruneJob?.cancel()
        pruneJob = null

        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to stop NSD discovery")
            }
        }
        discoveryListener = null
        isDiscovering = false
        deviceTimestamps.clear()
        _discoveredDevices.value = emptyList()
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {

            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Timber.tag(TAG).e("Resolve failed for %s: error=%d", info.serviceName, errorCode)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host?.hostAddress
                if (host.isNullOrEmpty()) {
                    // WHY: NSD can return entries with null hosts on some devices — skip them.
                    Timber.tag(TAG).w("Resolved service %s has null/empty host — ignoring", info.serviceName)
                    return
                }

                val device = DiscoveredDevice(
                    ip = host,
                    port = info.port,
                    name = info.serviceName
                )

                Timber.tag(TAG).i("Resolved device: %s @ %s:%d", device.name, device.ip, device.port)

                synchronized(deviceTimestamps) {
                    deviceTimestamps[device.ip] = System.currentTimeMillis()
                }

                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.ip == device.ip }
                current.add(device)
                _discoveredDevices.value = current
            }
        })
    }

    private fun removeDevice(serviceName: String) {
        val current = _discoveredDevices.value.toMutableList()
        val removed = current.removeAll { it.name == serviceName }
        if (removed) {
            _discoveredDevices.value = current
        }
    }

    private fun startPruning() {
        pruneJob?.cancel()
        pruneJob = scope.launch {
            while (true) {
                delay(PRUNE_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val staleIps = synchronized(deviceTimestamps) {
                    deviceTimestamps.filter { (_, lastSeen) ->
                        now - lastSeen > STALE_TIMEOUT_MS
                    }.keys.toSet()
                }

                if (staleIps.isNotEmpty()) {
                    Timber.tag(TAG).d("Pruning %d stale devices", staleIps.size)
                    synchronized(deviceTimestamps) {
                        staleIps.forEach { deviceTimestamps.remove(it) }
                    }
                    val current = _discoveredDevices.value.toMutableList()
                    current.removeAll { it.ip in staleIps }
                    _discoveredDevices.value = current
                }
            }
        }
    }
}
