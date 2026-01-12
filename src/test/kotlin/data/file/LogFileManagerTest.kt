package data.file

import domain.model.LogEntry
import domain.model.LogLevel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime

class LogFileManagerTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var logFileManager: LogFileManager
    private lateinit var testFile: File
    
    @BeforeEach
    fun setUp() {
        logFileManager = LogFileManager()
        testFile = tempDir.resolve("test_logs.txt").toFile()
    }
    
    @AfterEach
    fun tearDown() {
        if (testFile.exists()) {
            testFile.delete()
        }
    }
    
    // ==================== Export Tests ====================
    
    @Test
    fun `로그를 파일로 내보내기 성공`() = runTest {
        // Given
        val logs = createTestLogs()
        
        // When
        val result = logFileManager.exportLogs(logs, testFile)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(testFile.exists())
        val lines = testFile.readLines()
        assertEquals(3, lines.size)
    }
    
    @Test
    fun `내보낸 로그는 rawLine 포맷으로 저장된다`() = runTest {
        // Given
        val log = LogEntry(
            id = 1,
            timestamp = LocalDateTime.of(2024, 1, 10, 14, 23, 45, 123_000_000),
            pid = 1234,
            tid = 5678,
            level = LogLevel.DEBUG,
            tag = "TestTag",
            message = "Test message",
            rawLine = "01-10 14:23:45.123  1234  5678 D TestTag : Test message"
        )
        
        // When
        logFileManager.exportLogs(listOf(log), testFile)
        
        // Then
        val content = testFile.readText()
        assertTrue(content.contains("01-10 14:23:45.123  1234  5678 D TestTag : Test message"))
    }
    
    @Test
    fun `빈 로그 목록 내보내기`() = runTest {
        // Given
        val logs = emptyList<LogEntry>()
        
        // When
        val result = logFileManager.exportLogs(logs, testFile)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(testFile.exists())
        assertEquals("", testFile.readText())
    }
    
    @Test
    fun `잘못된 경로로 내보내기 실패`() = runTest {
        // Given
        val logs = createTestLogs()
        val invalidFile = File("/invalid/path/that/does/not/exist/logs.txt")
        
        // When
        val result = logFileManager.exportLogs(logs, invalidFile)
        
        // Then
        assertTrue(result.isFailure)
    }
    
    // ==================== Import Tests ====================
    
    @Test
    fun `파일에서 로그 불러오기 성공`() = runTest {
        // Given
        val logLines = """
            01-10 14:23:45.123  1234  5678 D TestTag : First message
            01-10 14:23:46.456  1234  5678 I TestTag : Second message
            01-10 14:23:47.789  1234  5678 W TestTag : Third message
        """.trimIndent()
        testFile.writeText(logLines)
        
        // When
        val result = logFileManager.importLogs(testFile)
        
        // Then
        assertTrue(result.isSuccess)
        val logs = result.getOrThrow()
        assertEquals(3, logs.size)
    }
    
    @Test
    fun `불러온 로그의 필드가 올바르게 파싱된다`() = runTest {
        // Given
        val logLine = "01-10 14:23:45.123  1234  5678 D TestTag : Test message"
        testFile.writeText(logLine)
        
        // When
        val result = logFileManager.importLogs(testFile)
        
        // Then
        assertTrue(result.isSuccess)
        val log = result.getOrThrow().first()
        assertEquals(1234, log.pid)
        assertEquals(5678, log.tid)
        assertEquals(LogLevel.DEBUG, log.level)
        assertEquals("TestTag", log.tag)
        assertEquals("Test message", log.message)
    }
    
    @Test
    fun `빈 파일 불러오기`() = runTest {
        // Given
        testFile.writeText("")
        
        // When
        val result = logFileManager.importLogs(testFile)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().size)
    }
    
    @Test
    fun `존재하지 않는 파일 불러오기 실패`() = runTest {
        // Given
        val nonExistentFile = File("/path/to/non/existent/file.txt")
        
        // When
        val result = logFileManager.importLogs(nonExistentFile)
        
        // Then
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `파싱 실패한 라인은 폴백으로 처리된다`() = runTest {
        // Given
        val mixedContent = """
            01-10 14:23:45.123  1234  5678 D TestTag : Valid line
            This is not a valid logcat line
            01-10 14:23:46.123  1234  5678 I TestTag : Another valid line
        """.trimIndent()
        testFile.writeText(mixedContent)
        
        // When
        val result = logFileManager.importLogs(testFile)
        
        // Then
        assertTrue(result.isSuccess)
        val logs = result.getOrThrow()
        assertEquals(3, logs.size)
        // 두 번째 라인은 폴백으로 파싱됨
        assertEquals("System", logs[1].tag)
    }
    
    // ==================== Round-trip Tests ====================
    
    @Test
    fun `내보내기 후 불러오기가 일관성 있게 동작한다`() = runTest {
        // Given
        val originalLogs = createTestLogs()
        
        // When
        logFileManager.exportLogs(originalLogs, testFile)
        val result = logFileManager.importLogs(testFile)
        
        // Then
        assertTrue(result.isSuccess)
        val importedLogs = result.getOrThrow()
        assertEquals(originalLogs.size, importedLogs.size)
        
        // 메시지와 태그가 동일한지 확인
        for (i in originalLogs.indices) {
            assertEquals(originalLogs[i].tag, importedLogs[i].tag)
            assertEquals(originalLogs[i].message, importedLogs[i].message)
            assertEquals(originalLogs[i].level, importedLogs[i].level)
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createTestLogs(): List<LogEntry> {
        return listOf(
            LogEntry(
                id = 1,
                timestamp = LocalDateTime.of(2024, 1, 10, 14, 23, 45, 123_000_000),
                pid = 1234,
                tid = 5678,
                level = LogLevel.DEBUG,
                tag = "TestTag",
                message = "First message",
                rawLine = "01-10 14:23:45.123  1234  5678 D TestTag : First message"
            ),
            LogEntry(
                id = 2,
                timestamp = LocalDateTime.of(2024, 1, 10, 14, 23, 46, 456_000_000),
                pid = 1234,
                tid = 5678,
                level = LogLevel.INFO,
                tag = "TestTag",
                message = "Second message",
                rawLine = "01-10 14:23:46.456  1234  5678 I TestTag : Second message"
            ),
            LogEntry(
                id = 3,
                timestamp = LocalDateTime.of(2024, 1, 10, 14, 23, 47, 789_000_000),
                pid = 1234,
                tid = 5678,
                level = LogLevel.WARN,
                tag = "TestTag",
                message = "Third message",
                rawLine = "01-10 14:23:47.789  1234  5678 W TestTag : Third message"
            )
        )
    }
}
