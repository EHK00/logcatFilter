package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.theme.AppColors

@Composable
fun StatusBar(
    totalLogs: Int,
    filteredLogs: Int,
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.SurfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatusItem(
                label = "Total",
                value = formatNumber(totalLogs)
            )
            
            StatusItem(
                label = "Filtered",
                value = formatNumber(filteredLogs)
            )
            
            Spacer(Modifier.weight(1f))
            
            Text(
                text = if (isCapturing) "● Capturing" else "○ Stopped",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCapturing) AppColors.LogInfo else AppColors.OnSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.OnSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.OnSurface
        )
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
