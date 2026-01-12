package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import domain.model.AppSettings
import domain.model.ColumnConfig
import domain.model.LogColumn
import ui.theme.AppColors
import ui.theme.MonoFontFamily
import java.io.File
import javax.swing.JFileChooser

/**
 * 애플리케이션 설정 다이얼로그
 */
@Composable
fun SettingsDialog(
    settings: AppSettings,
    columnConfig: ColumnConfig,
    onLogSaveDirectoryChange: (String) -> Unit,
    onAutoSaveOnStopChange: (Boolean) -> Unit,
    onColumnConfigChange: (ColumnConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var logSaveDir by remember { mutableStateOf(settings.logSaveDirectory) }
    var autoSaveOnStop by remember { mutableStateOf(settings.autoSaveOnStop) }
    var columnConfigText by remember { mutableStateOf(columnConfig.toConfigText()) }
    var columnConfigError by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = AppColors.Surface,
            tonalElevation = 8.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 제목
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.OnSurface
                )
                
                Divider(color = AppColors.OnSurface.copy(alpha = 0.1f))
                
                // ===== 컬럼 설정 섹션 =====
                Text(
                    text = "Column Display",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.OnSurface
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Edit to show/hide or reorder columns:",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.OnSurface.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = columnConfigText,
                        onValueChange = { newText ->
                            columnConfigText = newText
                            // 실시간 유효성 검사 및 적용
                            val newConfig = ColumnConfig.fromConfigText(newText)
                            if (newConfig.visibleColumns.isNotEmpty()) {
                                columnConfigError = null
                                onColumnConfigChange(newConfig)
                            } else {
                                columnConfigError = "At least one column must be visible"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = MonoFontFamily,
                            fontSize = 13.sp,
                            color = AppColors.OnSurface
                        ),
                        isError = columnConfigError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.OnSurface.copy(alpha = 0.3f),
                            errorBorderColor = AppColors.LogError,
                            focusedTextColor = AppColors.OnSurface,
                            unfocusedTextColor = AppColors.OnSurface
                        )
                    )
                    
                    // 에러 메시지
                    if (columnConfigError != null) {
                        Text(
                            text = columnConfigError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.LogError
                        )
                    }
                    
                    // 사용 가능한 태그 안내
                    Text(
                        text = "Available: {time} {package} {tid} {lvl} {tag} {message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.OnSurface.copy(alpha = 0.5f)
                    )
                    
                    // 리셋 버튼
                    TextButton(
                        onClick = {
                            columnConfigText = LogColumn.DEFAULT_CONFIG_TEXT
                            columnConfigError = null
                            onColumnConfigChange(ColumnConfig.default())
                        }
                    ) {
                        Text("Reset to Default", color = AppColors.Primary)
                    }
                }
                
                Divider(color = AppColors.OnSurface.copy(alpha = 0.1f))
                
                // ===== 로그 저장 섹션 =====
                Text(
                    text = "Log Auto-Save",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.OnSurface
                )
                
                // 자동 저장 활성화
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Auto-save on Stop",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.OnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = autoSaveOnStop,
                        onCheckedChange = { 
                            autoSaveOnStop = it
                            onAutoSaveOnStopChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.Primary,
                            checkedTrackColor = AppColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }
                
                // 저장 디렉토리
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Save Directory",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.OnSurface
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = logSaveDir,
                            onValueChange = { 
                                logSaveDir = it
                                onLogSaveDirectoryChange(it)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Primary,
                                unfocusedBorderColor = AppColors.OnSurface.copy(alpha = 0.3f),
                                focusedTextColor = AppColors.OnSurface,
                                unfocusedTextColor = AppColors.OnSurface
                            )
                        )
                        
                        Button(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = "Select Log Save Directory"
                                    currentDirectory = File(logSaveDir).let { 
                                        if (it.exists()) it else File(".")
                                    }
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    logSaveDir = chooser.selectedFile.absolutePath
                                    onLogSaveDirectoryChange(logSaveDir)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.SurfaceVariant
                            )
                        ) {
                            Text("Browse", color = AppColors.OnSurface)
                        }
                    }
                    
                    // 현재 절대 경로 표시
                    val absolutePath = remember(logSaveDir) {
                        try {
                            File(logSaveDir).absolutePath
                        } catch (e: Exception) {
                            logSaveDir
                        }
                    }
                    Text(
                        text = "→ $absolutePath",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.OnSurface.copy(alpha = 0.5f)
                    )
                }
                
                Divider(color = AppColors.OnSurface.copy(alpha = 0.1f))
                
                // 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = AppColors.Primary)
                    }
                }
            }
        }
    }
}
