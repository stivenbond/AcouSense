package com.dspcontroller.ui.telemetry

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * Telemetry Chart Screen — real-time line charts for CPU, Heap, and Signal RMS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TelemetryViewModel = hiltViewModel()
) {
    val telemetryWindow by viewModel.telemetryWindow.collectAsStateWithLifecycle()
    val chartWindowSeconds by viewModel.chartWindowSeconds.collectAsStateWithLifecycle()

    val windowOptions = listOf(10, 30, 60, 120)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telemetry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Window selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                windowOptions.forEach { seconds ->
                    FilterChip(
                        selected = chartWindowSeconds == seconds,
                        onClick = { viewModel.setChartWindow(seconds) },
                        label = { Text("${seconds}s") }
                    )
                }
            }

            // MPAndroidChart via AndroidView
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 8.dp),
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)
                        setDrawGridBackground(false)

                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.textColor = AndroidColor.GRAY

                        axisLeft.textColor = AndroidColor.GRAY
                        axisRight.isEnabled = false

                        legend.textColor = AndroidColor.GRAY
                        setBackgroundColor(AndroidColor.TRANSPARENT)

                        setNoDataText("Waiting for telemetry data…")
                        setNoDataTextColor(AndroidColor.GRAY)
                    }
                },
                update = { chart ->
                    if (telemetryWindow.isNotEmpty()) {
                        val baseTs = telemetryWindow.first().timestamp

                        val cpuEntries = telemetryWindow.mapIndexed { _, t ->
                            Entry(
                                ((t.timestamp - baseTs) / 1000f),
                                t.cpuPercent.toFloat()
                            )
                        }
                        val heapEntries = telemetryWindow.mapIndexed { _, t ->
                            Entry(
                                ((t.timestamp - baseTs) / 1000f),
                                (t.heapBytes / 1024f)
                            )
                        }
                        val rmsEntries = telemetryWindow.mapIndexed { _, t ->
                            Entry(
                                ((t.timestamp - baseTs) / 1000f),
                                t.signalRms * 100f
                            )
                        }

                        val cpuDataSet = LineDataSet(cpuEntries, "CPU (%)").apply {
                            color = AndroidColor.parseColor("#3D8BFF")
                            setCircleColor(AndroidColor.parseColor("#3D8BFF"))
                            lineWidth = 2f
                            circleRadius = 0f
                            setDrawCircles(false)
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }

                        val heapDataSet = LineDataSet(heapEntries, "Heap (KB)").apply {
                            color = AndroidColor.parseColor("#4CAF50")
                            setCircleColor(AndroidColor.parseColor("#4CAF50"))
                            lineWidth = 2f
                            circleRadius = 0f
                            setDrawCircles(false)
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }

                        val rmsDataSet = LineDataSet(rmsEntries, "RMS (×100)").apply {
                            color = AndroidColor.parseColor("#FF9800")
                            setCircleColor(AndroidColor.parseColor("#FF9800"))
                            lineWidth = 2f
                            circleRadius = 0f
                            setDrawCircles(false)
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }

                        chart.data = LineData(cpuDataSet, heapDataSet, rmsDataSet)
                        chart.invalidate()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics row
            if (telemetryWindow.isNotEmpty()) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        label = "CPU %",
                        min = telemetryWindow.minOf { it.cpuPercent }.toString(),
                        max = telemetryWindow.maxOf { it.cpuPercent }.toString(),
                        avg = "%.1f".format(telemetryWindow.map { it.cpuPercent }.average())
                    )
                    StatColumn(
                        label = "Heap KB",
                        min = (telemetryWindow.minOf { it.heapBytes } / 1024).toString(),
                        max = (telemetryWindow.maxOf { it.heapBytes } / 1024).toString(),
                        avg = "%.0f".format(telemetryWindow.map { it.heapBytes / 1024.0 }.average())
                    )
                    StatColumn(
                        label = "RMS",
                        min = "%.3f".format(telemetryWindow.minOf { it.signalRms }),
                        max = "%.3f".format(telemetryWindow.maxOf { it.signalRms }),
                        avg = "%.3f".format(telemetryWindow.map { it.signalRms.toDouble() }.average())
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    min: String,
    max: String,
    avg: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = "Min: $min", style = MaterialTheme.typography.bodySmall)
        Text(text = "Max: $max", style = MaterialTheme.typography.bodySmall)
        Text(text = "Avg: $avg", style = MaterialTheme.typography.bodySmall)
    }
}
