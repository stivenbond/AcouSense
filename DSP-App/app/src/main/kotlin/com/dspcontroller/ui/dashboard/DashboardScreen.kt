package com.dspcontroller.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dspcontroller.ui.components.ConnectionStatusBar
import com.dspcontroller.ui.components.MetricCard
import com.dspcontroller.ui.theme.SignalGreen
import com.dspcontroller.ui.theme.SignalRed
import com.dspcontroller.ui.theme.SignalYellow

/**
 * Main Dashboard Screen — telemetry overview, alerts, and navigation to sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToParameters: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToTelemetry: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDisconnect: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onDisconnect()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Disconnect"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            ConnectionStatusBar(connectionState = connectionState)

            // SYSTEM HEALTH SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "SYSTEM HEALTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cpuPct = telemetry?.cpuPercent ?: 0
                    val cpuColor = when {
                        cpuPct > 80 -> SignalRed
                        cpuPct > 60 -> SignalYellow
                        else -> SignalGreen
                    }
                    MetricCard(
                        title = "CPU",
                        value = "${cpuPct}%",
                        statusColor = cpuColor,
                        modifier = Modifier.weight(1f)
                    )

                    val heapKb = (telemetry?.heapBytes ?: 0L) / 1024
                    val heapColor = when {
                        heapKb < 50 -> SignalRed
                        heapKb < 100 -> SignalYellow
                        else -> SignalGreen
                    }
                    MetricCard(
                        title = "MEMORY",
                        value = "${heapKb}K",
                        statusColor = heapColor,
                        modifier = Modifier.weight(1f)
                    )

                    val rms = telemetry?.signalRms ?: 0f
                    val rmsColor = when {
                        rms > 0.9f -> SignalRed
                        rms > 0.7f -> SignalYellow
                        else -> SignalGreen
                    }
                    MetricCard(
                        title = "SIGNAL",
                        value = "%.2f".format(rms),
                        statusColor = rmsColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ALERT SECTION (Overlay style)
            if (activeAlerts.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeAlerts.forEach { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "CRITICAL: ${alert.metric.uppercase()} ${alert.condition} ${alert.threshold}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // COMMAND CENTER SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "COMMAND CENTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NavCard(
                            icon = Icons.Filled.Tune,
                            label = "PARAMETERS",
                            onClick = onNavigateToParameters,
                            modifier = Modifier.weight(1f)
                        )
                        NavCard(
                            icon = Icons.Filled.Save,
                            label = "PRESETS",
                            onClick = onNavigateToPresets,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NavCard(
                            icon = Icons.Filled.Timeline,
                            label = "ANALYTICS",
                            onClick = onNavigateToTelemetry,
                            modifier = Modifier.weight(1f)
                        )
                        NavCard(
                            icon = Icons.Filled.Settings,
                            label = "HARDWARE CFG",
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
