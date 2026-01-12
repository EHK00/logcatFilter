package domain.model

import java.time.LocalDateTime

data class LogEntry(
    val id: Long,
    val timestamp: LocalDateTime,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val rawLine: String,
    val packageName: String? = null
) {
    val formattedTime: String
        get() = "%02d-%02d %02d:%02d:%02d.%03d".format(
            timestamp.monthValue,
            timestamp.dayOfMonth,
            timestamp.hour,
            timestamp.minute,
            timestamp.second,
            timestamp.nano / 1_000_000
        )
    
    /**
     * 패키지명이 있으면 패키지명을, 없으면 PID를 반환합니다.
     */
    val displayProcess: String
        get() = packageName ?: pid.toString()
    
    /**
     * 패키지명을 설정한 새 LogEntry를 반환합니다.
     */
    fun withPackageName(name: String?): LogEntry = copy(packageName = name)
}
