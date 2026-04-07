package com.dspcontroller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.ui.theme.SignalGreen
import com.dspcontroller.ui.theme.SignalRed
import com.dspcontroller.ui.theme.SignalYellow

/**
 * Animated status bar indicating the current WebSocket connection state.
 *
 * Displays a color-coded dot and label:
 * - Green = Connected
 * - Yellow = Connecting
 * - Red = Disconnected or Error
 */
@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (connectionState) {
        is ConnectionState.Connected -> SignalGreen to "Connected"
        is ConnectionState.Connecting -> SignalYellow to "Connecting…"
        is ConnectionState.Disconnected -> SignalRed to "Disconnected"
        is ConnectionState.Error -> SignalRed to "Error: ${connectionState.cause?.message ?: "Unknown"}"
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 500),
        label = "connectionColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
