package data.repository

import domain.model.LogEntry
import domain.model.LogLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogRepositoryTest {
    
    private lateinit var repository: LogRepository
    
    private fun createLogEntry(id: Long) = LogEntry(
        id = id,
        timestamp = LocalDateTime.now(),
        pid = 1234,
        tid = 5678,
        level = LogLevel.DEBUG,
        tag = "TestTag",
        message = "Test message $id",
        rawLine = ""
    )
    
    @BeforeEach
    fun setup() {
        repository = LogRepository(maxSize = 100)
    }
    
    @Test
    fun `로그 추가 후 조회`() = runTest {
        // Given
        val entry = createLogEntry(1)
        
        // When
        repository.addLog(entry)
        
        // Then
        val logs = repository.logs.first()
        assertEquals(1, logs.size)
        assertEquals(entry, logs[0])
    }
    
    @Test
    fun `여러 로그 추가 후 조회`() = runTest {
        // Given
        val entries = (1L..5L).map { createLogEntry(it) }
        
        // When
        entries.forEach { repository.addLog(it) }
        
        // Then
        val logs = repository.logs.first()
        assertEquals(5, logs.size)
        assertEquals(entries, logs)
    }
    
    @Test
    fun `일괄 로그 추가`() = runTest {
        // Given
        val entries = (1L..10L).map { createLogEntry(it) }
        
        // When
        repository.addLogs(entries)
        
        // Then
        val logs = repository.logs.first()
        assertEquals(10, logs.size)
        assertEquals(entries, logs)
    }
    
    @Test
    fun `로그 클리어`() = runTest {
        // Given
        repository.addLogs((1L..5L).map { createLogEntry(it) })
        
        // When
        repository.clear()
        
        // Then
        val logs = repository.logs.first()
        assertTrue(logs.isEmpty())
        assertEquals(0, repository.size())
    }
    
    @Test
    fun `maxSize 초과시 오래된 로그 제거`() = runTest {
        // Given - maxSize가 100인 repository
        val entries = (1L..150L).map { createLogEntry(it) }
        
        // When
        repository.addLogs(entries)
        
        // Then
        val logs = repository.logs.first()
        assertEquals(100, logs.size)
        assertEquals(100, repository.size())
        
        // 첫 번째 로그는 ID가 51이어야 함 (1-50이 제거됨)
        assertEquals(51L, logs.first().id)
        assertEquals(150L, logs.last().id)
    }
    
    @Test
    fun `maxSize 초과시 단일 추가로도 오래된 로그 제거`() = runTest {
        // Given - 100개 채우기
        repository.addLogs((1L..100L).map { createLogEntry(it) })
        
        // When - 1개 더 추가
        repository.addLog(createLogEntry(101))
        
        // Then
        val logs = repository.logs.first()
        assertEquals(100, logs.size)
        assertEquals(2L, logs.first().id) // ID 1이 제거됨
        assertEquals(101L, logs.last().id)
    }
    
    @Test
    fun `size 메서드 정확성`() = runTest {
        // Given
        assertEquals(0, repository.size())
        
        // When
        repository.addLog(createLogEntry(1))
        assertEquals(1, repository.size())
        
        repository.addLogs((2L..5L).map { createLogEntry(it) })
        assertEquals(5, repository.size())
        
        repository.clear()
        assertEquals(0, repository.size())
    }
    
    @Test
    fun `빈 리스트 추가시 상태 변경 없음`() = runTest {
        // Given
        repository.addLog(createLogEntry(1))
        
        // When
        repository.addLogs(emptyList())
        
        // Then
        assertEquals(1, repository.size())
    }
    
    @Test
    fun `커스텀 maxSize 설정`() = runTest {
        // Given
        val smallRepository = LogRepository(maxSize = 5)
        
        // When
        smallRepository.addLogs((1L..10L).map { createLogEntry(it) })
        
        // Then
        val logs = smallRepository.logs.first()
        assertEquals(5, logs.size)
        assertEquals(6L, logs.first().id)
        assertEquals(10L, logs.last().id)
    }
}
