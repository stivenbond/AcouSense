package com.dspcontroller.domain.model

/**
 * Represents a device discovered via mDNS on the local network.
 *
 * @property ip The resolved IP address of the device.
 * @property port The port the DSP service is running on.
 * @property name The mDNS service name advertised by the device.
 */
data class DiscoveredDevice(
    val ip: String,
    val port: Int,
    val name: String
)
