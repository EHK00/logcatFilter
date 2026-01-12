package data.file

import data.parser.LogParser
import domain.model.LogEntry
import java.io.File

/**
 * 로그 파일의 내보내기/불러오기를 담당합니다.
 */
class LogFileManager {
    
    private val logParser = LogParser()
    
    /**
     * 로그를 파일로 내보냅니다.
     * 
     * @param logs 내보낼 로그 목록
     * @param file 저장할 파일
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure(Exception)
     */
    fun exportLogs(logs: List<LogEntry>, file: File): Result<Unit> {
        return try {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                logs.forEach { log ->
                    writer.write(log.rawLine)
                    writer.newLine()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 파일에서 로그를 불러옵니다.
     * 
     * @param file 불러올 파일
     * @return 성공 시 Result.success(List<LogEntry>), 실패 시 Result.failure(Exception)
     */
    fun importLogs(file: File): Result<List<LogEntry>> {
        return try {
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("File does not exist: ${file.path}"))
            }
            
            logParser.reset()
            
            val logs = file.bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    if (line.isNotBlank()) {
                        logParser.parse(line)
                    } else {
                        null
                    }
                }.toList()
            }
            
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 지원하는 파일 확장자 목록
     */
    companion object {
        val SUPPORTED_EXTENSIONS = listOf("txt", "log")
        const val DEFAULT_EXTENSION = "txt"
        
        /**
         * 기본 파일명 생성 (타임스탬프 기반)
         */
        fun generateDefaultFileName(): String {
            val timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            return "logcat_$timestamp.$DEFAULT_EXTENSION"
        }
    }
}
