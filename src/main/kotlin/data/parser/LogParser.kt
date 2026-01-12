package data.parser

import domain.model.LogEntry
import domain.model.LogLevel
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

/**
 * Logcat threadtime 포맷을 파싱합니다.
 * 
 * 포맷: "01-10 14:23:45.123  1234  5678 D TagName : Log message content"
 */
class LogParser {
    
    private val idCounter = AtomicLong(0)
    
    // Logcat threadtime 포맷 정규표현식
    // 01-10 14:23:45.123  1234  5678 D TagName : Message
    private val threadTimePattern = Regex(
        """^(\d{2})-(\d{2})\s+(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.+?)\s*:\s*(.*)$"""
    )
    
    /**
     * 로그 라인을 파싱합니다.
     * 파싱 실패 시 null을 반환합니다.
     */
    fun parse(line: String): LogEntry? {
        if (line.isBlank()) return null
        
        val match = threadTimePattern.find(line) ?: return createFallbackEntry(line)
        
        return try {
            val groups = match.groupValues
            val month = groups[1]
            val day = groups[2]
            val hour = groups[3]
            val minute = groups[4]
            val second = groups[5]
            val millis = groups[6]
            val pid = groups[7]
            val tid = groups[8]
            val levelChar = groups[9]
            val tag = groups[10]
            val message = groups[11]
            
            val level = LogLevel.fromChar(levelChar[0]) ?: LogLevel.VERBOSE
            
            val timestamp = LocalDateTime.of(
                LocalDateTime.now().year,
                month.toInt(),
                day.toInt(),
                hour.toInt(),
                minute.toInt(),
                second.toInt(),
                millis.toInt() * 1_000_000
            )
            
            LogEntry(
                id = idCounter.incrementAndGet(),
                timestamp = timestamp,
                pid = pid.toInt(),
                tid = tid.toInt(),
                level = level,
                tag = tag.trim(),
                message = message,
                rawLine = line
            )
        } catch (e: Exception) {
            createFallbackEntry(line)
        }
    }
    
    /**
     * 파싱 실패 시 폴백 엔트리를 생성합니다.
     */
    private fun createFallbackEntry(line: String): LogEntry? {
        // 완전히 빈 줄이거나 시스템 메시지는 스킵
        if (line.isBlank() || line.startsWith("-----")) return null
        
        return LogEntry(
            id = idCounter.incrementAndGet(),
            timestamp = LocalDateTime.now(),
            pid = 0,
            tid = 0,
            level = LogLevel.VERBOSE,
            tag = "System",
            message = line,
            rawLine = line
        )
    }
    
    /**
     * ID 카운터를 리셋합니다.
     */
    fun reset() {
        idCounter.set(0)
    }
}
