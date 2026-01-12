package ui.viewmodel

import data.adb.AdbService
import data.file.LogFileManager
import data.parser.LogParser
import data.repository.LogRepository
import data.repository.SettingsRepository
import domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * 메인 화면의 상태를 관리하는 ViewModel입니다.
 */
class MainViewModel {
    
    companion object {
        /** 프로세스 맵 갱신 주기 (ms) */
        private const val PROCESS_MAP_POLL_INTERVAL_MS = 2000L
        
        /** 성공 메시지 표시 시간 (ms) */
        private const val SUCCESS_MESSAGE_DURATION_MS = 3000L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val adbService: AdbService? = try {
        AdbService()
    } catch (e: Exception) {
        null
    }
    
    private val logParser = LogParser()
    private val logRepository = LogRepository()
    private val logFileManager = LogFileManager()
    private val settingsRepository = SettingsRepository()
    
    // 앱 설정
    private val _appSettings = MutableStateFlow(settingsRepository.load())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    // UI 상태
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // 디바이스 목록
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()
    
    // 선택된 디바이스
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()
    
    // 필터 쿼리 (통합 검색)
    private val _filterQuery = MutableStateFlow("")
    val filterQuery: StateFlow<String> = _filterQuery.asStateFlow()
    
    // 파싱된 필터 쿼리
    private val parsedFilterQuery: StateFlow<FilterQuery> = _filterQuery
        .map { FilterQuery.parse(it) }
        .stateIn(scope, SharingStarted.Eagerly, FilterQuery())
    
    // 검색 쿼리 (필터와 별도, 하이라이트용)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 현재 검색 인덱스 (1-based, 0이면 검색 결과 없음)
    private val _currentSearchIndex = MutableStateFlow(0)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex.asStateFlow()
    
    // 검색 결과 매칭 수
    private val _searchMatchCount = MutableStateFlow(0)
    val searchMatchCount: StateFlow<Int> = _searchMatchCount.asStateFlow()
    
    // 검색 결과 위치 (로그 인덱스 목록)
    private val _searchMatchIndices = MutableStateFlow<List<Int>>(emptyList())
    val searchMatchIndices: StateFlow<List<Int>> = _searchMatchIndices.asStateFlow()
    
    // 하이라이트 대상 검색어 (검색 쿼리 기반)
    val highlightTerms: StateFlow<List<String>> = _searchQuery
        .map { query -> if (query.isNotBlank()) listOf(query) else emptyList() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    
    // PID-패키지명 매핑
    private val _processMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val processMap: StateFlow<Map<Int, String>> = _processMap.asStateFlow()
    
    // 전체 로그 (패키지명 매핑 적용)
    val allLogs: StateFlow<List<LogEntry>> = combine(
        logRepository.logs,
        _processMap
    ) { logs, processMapping ->
        logs.map { entry ->
            val packageName = processMapping[entry.pid]
            if (packageName != null && entry.packageName != packageName) {
                entry.withPackageName(packageName)
            } else {
                entry
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    
    // 필터링된 로그
    val filteredLogs: StateFlow<List<LogEntry>> = combine(
        allLogs,
        parsedFilterQuery
    ) { logs, query ->
        logs.filter { query.matches(it) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    
    // 에러 메시지
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 컬럼 설정
    private val _columnConfig = MutableStateFlow(ColumnConfig())
    val columnConfig: StateFlow<ColumnConfig> = _columnConfig.asStateFlow()
    
    // 자동 스크롤 설정
    private val _autoScrollEnabled = MutableStateFlow(true)
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled.asStateFlow()
    
    private var logcatJob: Job? = null
    private var processMapJob: Job? = null
    
    init {
        // ADB 사용 가능 여부 체크
        if (adbService == null) {
            _errorMessage.value = "ADB를 찾을 수 없습니다. ANDROID_HOME 또는 PATH를 확인하세요."
        } else {
            startDevicePolling()
        }
    }
    
    /**
     * 디바이스 목록을 주기적으로 갱신합니다.
     */
    private fun startDevicePolling() {
        scope.launch {
            adbService?.observeDevices()?.collect { deviceList ->
                _devices.value = deviceList.filter { it.state == DeviceState.ONLINE }
                
                // 선택된 디바이스가 연결 해제되면 선택 해제
                val selected = _selectedDevice.value
                if (selected != null && deviceList.none { it.id == selected.id && it.state == DeviceState.ONLINE }) {
                    _selectedDevice.value = null
                    stopCapture()
                }
            }
        }
    }
    
    /**
     * 디바이스를 선택합니다.
     */
    fun selectDevice(device: Device?) {
        if (_uiState.value.isCapturing) {
            stopCapture()
        }
        _selectedDevice.value = device
    }
    
    /**
     * 로그 캡처를 시작합니다.
     */
    fun startCapture() {
        val device = _selectedDevice.value ?: return
        if (_uiState.value.isCapturing) return
        
        // 기존 로그 클리어
        clearLogs()
        
        _uiState.value = _uiState.value.copy(isCapturing = true)
        
        // Logcat 캡처 시작
        logcatJob = scope.launch {
            adbService?.startLogcat(device.id, this)
                ?.collect { line ->
                    val entry = logParser.parse(line)
                    if (entry != null) {
                        logRepository.addLog(entry)
                    }
                }
        }
        
        // PID-패키지명 매핑 주기적 갱신 시작
        startProcessMapPolling(device.id)
    }
    
    /**
     * PID-패키지명 매핑을 주기적으로 갱신합니다.
     */
    private fun startProcessMapPolling(deviceId: String) {
        processMapJob?.cancel()
        processMapJob = scope.launch {
            // 초기 로딩
            _processMap.value = adbService?.getProcessMap(deviceId) ?: emptyMap()
            
            // 주기적 갱신 (2초마다)
            adbService?.observeProcessMap(deviceId, PROCESS_MAP_POLL_INTERVAL_MS)?.collect { mapping ->
                _processMap.value = mapping
            }
        }
    }
    
    /**
     * 로그 캡처를 중지합니다.
     * autoSaveOnStop 설정이 켜져 있으면 자동으로 로그를 저장합니다.
     */
    fun stopCapture() {
        val wasCapturing = _uiState.value.isCapturing
        
        logcatJob?.cancel()
        logcatJob = null
        processMapJob?.cancel()
        processMapJob = null
        adbService?.stopLogcat()
        _uiState.value = _uiState.value.copy(isCapturing = false)
        
        // 자동 저장 (캡처 중이었고, 설정이 켜져 있고, 로그가 있을 때)
        if (wasCapturing && _appSettings.value.autoSaveOnStop && allLogs.value.isNotEmpty()) {
            autoSaveLogs()
        }
    }
    
    /**
     * 로그를 자동 저장합니다.
     */
    private fun autoSaveLogs() {
        val settings = _appSettings.value
        val saveDir = File(settings.logSaveDirectory)
        
        // 디렉토리 생성
        if (!saveDir.exists()) {
            saveDir.mkdirs()
        }
        
        val fileName = LogFileManager.generateDefaultFileName()
        val file = File(saveDir, fileName)
        
        val logs = filteredLogs.value
        val result = logFileManager.exportLogs(logs, file)
        
        if (result.isSuccess) {
            // 저장 성공 알림 (에러 메시지 영역 활용, 잠시 후 사라짐)
            _errorMessage.value = "로그 저장됨: ${file.absolutePath}"
            scope.launch {
                delay(SUCCESS_MESSAGE_DURATION_MS)
                if (_errorMessage.value?.startsWith("로그 저장됨") == true) {
                    _errorMessage.value = null
                }
            }
        } else {
            _errorMessage.value = "자동 저장 실패: ${result.exceptionOrNull()?.message}"
        }
    }
    
    /**
     * 로그를 클리어합니다.
     */
    fun clearLogs() {
        logRepository.clear()
        logParser.reset()
    }
    
    /**
     * 디바이스의 Logcat 버퍼를 클리어합니다.
     */
    fun clearDeviceLogcat() {
        val device = _selectedDevice.value ?: return
        scope.launch {
            adbService?.clearLogcat(device.id)
            clearLogs()
        }
    }
    
    /**
     * 필터 쿼리를 업데이트합니다.
     */
    fun updateFilterQuery(query: String) {
        _filterQuery.value = query
    }
    
    /**
     * 에러 메시지를 클리어합니다.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 컬럼 너비를 업데이트합니다.
     */
    fun updateColumnWidth(column: LogColumn, width: androidx.compose.ui.unit.Dp) {
        _columnConfig.value = _columnConfig.value.setWidth(column, width)
    }
    
    /**
     * 컬럼 표시/숨김을 설정합니다.
     */
    fun setColumnVisibility(column: LogColumn, visible: Boolean) {
        _columnConfig.value = _columnConfig.value.setVisibility(column, visible)
    }
    
    /**
     * 컬럼 설정을 업데이트합니다.
     */
    fun updateColumnConfig(config: ColumnConfig) {
        _columnConfig.value = config
    }
    
    /**
     * 자동 스크롤을 토글합니다.
     */
    fun toggleAutoScroll() {
        _autoScrollEnabled.value = !_autoScrollEnabled.value
    }
    
    /**
     * 자동 스크롤을 설정합니다.
     */
    fun setAutoScroll(enabled: Boolean) {
        _autoScrollEnabled.value = enabled
    }
    
    // =========== 검색 기능 (필터와 별도) ===========
    
    /**
     * 검색어를 업데이트합니다.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        updateSearchMatches()
    }
    
    /**
     * 검색을 클리어합니다.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _currentSearchIndex.value = 0
        _searchMatchCount.value = 0
        _searchMatchIndices.value = emptyList()
    }
    
    /**
     * 다음 검색 결과로 이동합니다.
     */
    fun goToNextMatch(): Int? {
        val indices = _searchMatchIndices.value
        if (indices.isEmpty()) return null
        
        val currentIdx = _currentSearchIndex.value
        val newIdx = if (currentIdx >= indices.size) 1 else currentIdx + 1
        _currentSearchIndex.value = newIdx
        return indices.getOrNull(newIdx - 1)
    }
    
    /**
     * 이전 검색 결과로 이동합니다.
     */
    fun goToPrevMatch(): Int? {
        val indices = _searchMatchIndices.value
        if (indices.isEmpty()) return null
        
        val currentIdx = _currentSearchIndex.value
        val newIdx = if (currentIdx <= 1) indices.size else currentIdx - 1
        _currentSearchIndex.value = newIdx
        return indices.getOrNull(newIdx - 1)
    }
    
    /**
     * 검색 매칭 인덱스를 업데이트합니다.
     */
    private fun updateSearchMatches() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _searchMatchIndices.value = emptyList()
            _searchMatchCount.value = 0
            _currentSearchIndex.value = 0
            return
        }
        
        val logs = filteredLogs.value
        val matchIndices = logs.mapIndexedNotNull { index, entry ->
            val matches = entry.message.contains(query, ignoreCase = true) ||
                         entry.tag.contains(query, ignoreCase = true)
            if (matches) index else null
        }
        
        _searchMatchIndices.value = matchIndices
        _searchMatchCount.value = matchIndices.size
        _currentSearchIndex.value = if (matchIndices.isNotEmpty()) 1 else 0
    }
    
    /**
     * 검색 결과를 갱신합니다 (로그가 변경될 때 호출).
     */
    fun refreshSearchMatches() {
        if (_searchQuery.value.isNotBlank()) {
            updateSearchMatches()
        }
    }
    
    // =========== 파일 내보내기/불러오기 ===========
    
    /**
     * 현재 로그를 파일로 내보냅니다.
     * 
     * @param file 저장할 파일
     * @return 성공 시 true, 실패 시 false
     */
    fun exportLogs(file: File): Boolean {
        val logs = filteredLogs.value
        val result = logFileManager.exportLogs(logs, file)
        
        if (result.isFailure) {
            _errorMessage.value = "로그 내보내기 실패: ${result.exceptionOrNull()?.message}"
            return false
        }
        return true
    }
    
    /**
     * 파일에서 로그를 불러옵니다.
     * 기존 로그를 대체합니다.
     * 
     * @param file 불러올 파일
     * @return 성공 시 true, 실패 시 false
     */
    fun importLogs(file: File): Boolean {
        if (_uiState.value.isCapturing) {
            stopCapture()
        }
        
        val result = logFileManager.importLogs(file)
        
        if (result.isFailure) {
            _errorMessage.value = "로그 불러오기 실패: ${result.exceptionOrNull()?.message}"
            return false
        }
        
        // 기존 로그 클리어 후 불러온 로그 추가
        logRepository.clear()
        logParser.reset()
        result.getOrNull()?.let { logs ->
            logRepository.addLogs(logs)
        }
        
        return true
    }
    
    /**
     * 기본 파일명을 생성합니다.
     */
    fun generateDefaultFileName(): String {
        return LogFileManager.generateDefaultFileName()
    }
    
    // =========== 설정 관리 ===========
    
    /**
     * 로그 저장 디렉토리를 업데이트합니다.
     */
    fun updateLogSaveDirectory(path: String) {
        val updated = _appSettings.value.copy(logSaveDirectory = path)
        _appSettings.value = updated
        settingsRepository.save(updated)
    }
    
    /**
     * Stop 시 자동 저장 설정을 업데이트합니다.
     */
    fun updateAutoSaveOnStop(enabled: Boolean) {
        val updated = _appSettings.value.copy(autoSaveOnStop = enabled)
        _appSettings.value = updated
        settingsRepository.save(updated)
    }
    
    /**
     * ViewModel을 정리합니다.
     */
    fun dispose() {
        stopCapture()
        scope.cancel()
    }
}

data class MainUiState(
    val isCapturing: Boolean = false,
    val isLoading: Boolean = false
)
