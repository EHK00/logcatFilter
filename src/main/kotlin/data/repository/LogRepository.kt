package data.repository

import domain.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그 데이터를 관리하는 Repository입니다.
 */
class LogRepository(
    private val maxSize: Int = 100_000
) {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private val buffer = ArrayDeque<LogEntry>()
    
    /**
     * 새 로그 엔트리를 추가합니다.
     */
    @Synchronized
    fun addLog(entry: LogEntry) {
        if (buffer.size >= maxSize) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
        _logs.value = buffer.toList()
    }
    
    /**
     * 여러 로그 엔트리를 일괄 추가합니다.
     */
    @Synchronized
    fun addLogs(entries: List<LogEntry>) {
        entries.forEach { entry ->
            if (buffer.size >= maxSize) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
        }
        _logs.value = buffer.toList()
    }
    
    /**
     * 모든 로그를 클리어합니다.
     */
    @Synchronized
    fun clear() {
        buffer.clear()
        _logs.value = emptyList()
    }
    
    /**
     * 현재 로그 개수를 반환합니다.
     */
    fun size(): Int = buffer.size
}
