package com.dspcontroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dspcontroller.ui.theme.SignalGreen
import com.dspcontroller.ui.theme.SignalYellow

/**
 * Card Composable for displaying a preset in a list.
 *
 * Shows the preset name, description, sync status badge,
 * and action buttons for loading and deleting.
 *
 * @param name The preset name.
 * @param description Optional description text.
 * @param isSynced Whether the preset is synced to the device.
 * @param onLoad Callback when the Load button is tapped.
 * @param onDelete Callback when the Delete button is tapped.
 */
@Composable
fun PresetCard(
    name: String,
    description: String,
    isSynced: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = if (isSynced) "Synced" else "Local only",
                        tint = if (isSynced) SignalGreen else SignalYellow,
                        modifier = Modifier.height(16.dp)
                    )
                }
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Row {
                IconButton(onClick = onLoad) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Load preset",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete preset",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
