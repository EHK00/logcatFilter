package ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import data.file.LogFileManager
import ui.components.*
import ui.theme.AppColors
import ui.theme.LogcatFilterTheme
import ui.viewmodel.MainViewModel
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun MainWindow(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val filterQuery by viewModel.filterQuery.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val columnConfig by viewModel.columnConfig.collectAsState()
    val autoScrollEnabled by viewModel.autoScrollEnabled.collectAsState()
    val highlightTerms by viewModel.highlightTerms.collectAsState()
    
    // 검색 관련 상태
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMatchCount by viewModel.searchMatchCount.collectAsState()
    val currentSearchIndex by viewModel.currentSearchIndex.collectAsState()
    
    // 설정 다이얼로그 표시 상태
    var showSettings by remember { mutableStateOf(false) }
    
    // 앱 설정
    val appSettings by viewModel.appSettings.collectAsState()
    
    // 검색 바 표시 상태
    var showSearchBar by remember { mutableStateOf(false) }
    
    // 스크롤 타겟 인덱스
    var scrollToIndex by remember { mutableStateOf<Int?>(null) }
    
    // 메인 윈도우 포커스 요청자 (글로벌 단축키용)
    val mainFocusRequester = remember { FocusRequester() }
    
    // 앱 시작 시 메인 윈도우에 포커스
    LaunchedEffect(Unit) {
        mainFocusRequester.requestFocus()
    }
    
    // 파일 내보내기 함수
    val exportLogs: () -> Unit = {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "로그 내보내기"
            selectedFile = File(viewModel.generateDefaultFileName())
            fileFilter = FileNameExtensionFilter(
                "Log files (*.txt, *.log)",
                *LogFileManager.SUPPORTED_EXTENSIONS.toTypedArray()
            )
        }
        
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            // 확장자가 없으면 기본 확장자 추가
            if (!file.name.contains(".")) {
                file = File(file.path + ".${LogFileManager.DEFAULT_EXTENSION}")
            }
            viewModel.exportLogs(file)
        }
    }
    
    // 파일 불러오기 함수
    val importLogs: () -> Unit = {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "로그 불러오기"
            fileFilter = FileNameExtensionFilter(
                "Log files (*.txt, *.log)",
                *LogFileManager.SUPPORTED_EXTENSIONS.toTypedArray()
            )
        }
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            viewModel.importLogs(fileChooser.selectedFile)
        }
    }
    
    LogcatFilterTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(mainFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.F &&
                        (event.isMetaPressed || event.isCtrlPressed)
                    ) {
                        if (event.isShiftPressed) {
                            // Cmd+Shift+F / Ctrl+Shift+F → 이전 검색 결과로 이동
                            if (searchMatchCount > 0) {
                                viewModel.goToPrevMatch()?.let { index ->
                                    scrollToIndex = index
                                    viewModel.setAutoScroll(false)
                                }
                            }
                        } else {
                            // Cmd+F / Ctrl+F → 검색 바 열기 또는 다음 결과로 이동
                            if (!showSearchBar) {
                                showSearchBar = true
                            } else if (searchMatchCount > 0) {
                                viewModel.goToNextMatch()?.let { index ->
                                    scrollToIndex = index
                                    viewModel.setAutoScroll(false)
                                }
                            }
                        }
                        true
                    } else {
                        false
                    }
                },
            color = AppColors.Background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 에러 배너
                errorMessage?.let { error ->
                    ErrorBanner(
                        message = error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                
                // 툴바
                ToolBar(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    isCapturing = uiState.isCapturing,
                    hasLogs = allLogs.isNotEmpty(),
                    onDeviceSelected = { viewModel.selectDevice(it) },
                    onStartCapture = { viewModel.startCapture() },
                    onStopCapture = { viewModel.stopCapture() },
                    onClearLogs = { viewModel.clearLogs() },
                    onExportLogs = exportLogs,
                    onImportLogs = importLogs,
                    onOpenSearch = { showSearchBar = true },
                    onOpenSettings = { showSettings = true }
                )
                
                // 필터 패널 (통합 검색)
                FilterPanel(
                    query = filterQuery,
                    onQueryChange = { viewModel.updateFilterQuery(it) }
                )
                
                // 검색 바 (필터와 별도)
                if (showSearchBar) {
                    SearchBar(
                        query = searchQuery,
                        matchCount = searchMatchCount,
                        currentIndex = currentSearchIndex,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onPrevMatch = {
                            viewModel.goToPrevMatch()?.let { index ->
                                scrollToIndex = index
                                viewModel.setAutoScroll(false)
                            }
                        },
                        onNextMatch = {
                            viewModel.goToNextMatch()?.let { index ->
                                scrollToIndex = index
                                viewModel.setAutoScroll(false)
                            }
                        },
                        onClose = {
                            showSearchBar = false
                            viewModel.clearSearch()
                            mainFocusRequester.requestFocus()
                        }
                    )
                }
                
                // 로그 테이블
                LogTable(
                    logs = filteredLogs,
                    columnConfig = columnConfig,
                    autoScrollEnabled = autoScrollEnabled,
                    highlightTerms = highlightTerms,
                    scrollToIndex = scrollToIndex,
                    onScrollComplete = { scrollToIndex = null },
                    onColumnResize = { column, width ->
                        viewModel.updateColumnWidth(column, width)
                    },
                    onAutoScrollChange = { enabled ->
                        viewModel.setAutoScroll(enabled)
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // 상태바
                StatusBar(
                    totalLogs = allLogs.size,
                    filteredLogs = filteredLogs.size,
                    isCapturing = uiState.isCapturing
                )
            }
        }
        
        // 설정 다이얼로그
        if (showSettings) {
            SettingsDialog(
                settings = appSettings,
                columnConfig = columnConfig,
                onLogSaveDirectoryChange = { viewModel.updateLogSaveDirectory(it) },
                onAutoSaveOnStopChange = { viewModel.updateAutoSaveOnStop(it) },
                onColumnConfigChange = { viewModel.updateColumnConfig(it) },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = AppColors.LogError.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                color = AppColors.LogError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = AppColors.LogError)
            }
        }
    }
}
