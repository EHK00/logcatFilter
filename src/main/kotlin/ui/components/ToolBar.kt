package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.Device
import ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBar(
    devices: List<Device>,
    selectedDevice: Device?,
    isCapturing: Boolean,
    hasLogs: Boolean,
    onDeviceSelected: (Device?) -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onImportLogs: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.Surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 디바이스 선택 드롭다운
            DeviceSelector(
                devices = devices,
                selectedDevice = selectedDevice,
                onDeviceSelected = onDeviceSelected,
                enabled = !isCapturing,
                modifier = Modifier.width(280.dp)
            )
            
            // Start/Stop 버튼
            if (isCapturing) {
                FilledTonalButton(
                    onClick = onStopCapture,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AppColors.LogError.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            } else {
                FilledTonalButton(
                    onClick = onStartCapture,
                    enabled = selectedDevice != null,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AppColors.LogInfo.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Start")
                }
            }
            
            // Clear 버튼
            OutlinedButton(
                onClick = onClearLogs
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Clear")
            }
            
            // 구분선
            Divider(
                modifier = Modifier.height(24.dp).width(1.dp),
                color = AppColors.Border
            )
            
            // Export 버튼
            IconButton(
                onClick = onExportLogs,
                enabled = hasLogs
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export Logs",
                    modifier = Modifier.size(20.dp),
                    tint = if (hasLogs) AppColors.OnSurface.copy(alpha = 0.7f) else AppColors.OnSurface.copy(alpha = 0.3f)
                )
            }
            
            // Import 버튼
            IconButton(
                onClick = onImportLogs,
                enabled = !isCapturing
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Import Logs",
                    modifier = Modifier.size(20.dp),
                    tint = if (!isCapturing) AppColors.OnSurface.copy(alpha = 0.7f) else AppColors.OnSurface.copy(alpha = 0.3f)
                )
            }
            
            // 구분선
            Divider(
                modifier = Modifier.height(24.dp).width(1.dp),
                color = AppColors.Border
            )
            
            // 검색 버튼
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search (Ctrl+F)",
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }
            
            // 설정 버튼
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(Modifier.weight(1f))
            
            // 캡처 상태 표시
            if (isCapturing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.LogInfo
                    )
                    Text(
                        text = "Capturing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.LogInfo
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelector(
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceSelected: (Device?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDevice?.displayName ?: "Select Device",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (devices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No devices connected") },
                    onClick = { },
                    enabled = false
                )
            } else {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(device.displayName)
                                Text(
                                    text = if (device.type == domain.model.DeviceType.EMULATOR) "Emulator" else "Device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        onClick = {
                            onDeviceSelected(device)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
