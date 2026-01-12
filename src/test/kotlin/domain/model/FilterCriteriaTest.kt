package domain.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterCriteriaTest {
    
    private fun createLogEntry(
        pid: Int = 1234,
        tid: Int = 5678,
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "TestTag",
        message: String = "Test message",
        packageName: String? = null
    ) = LogEntry(
        id = 1,
        timestamp = LocalDateTime.now(),
        pid = pid,
        tid = tid,
        level = level,
        tag = tag,
        message = message,
        rawLine = "",
        packageName = packageName
    )
    
    // 로그 레벨 필터링 테스트
    
    @Test
    fun `모든 레벨이 활성화되면 모든 로그 통과`() {
        val criteria = FilterCriteria(levels = LogLevel.entries.toSet())
        
        LogLevel.entries.forEach { level ->
            val entry = createLogEntry(level = level)
            assertTrue(criteria.matches(entry), "Level $level should match")
        }
    }
    
    @Test
    fun `특정 레벨만 활성화시 해당 레벨만 통과`() {
        val criteria = FilterCriteria(levels = setOf(LogLevel.ERROR, LogLevel.WARN))
        
        assertTrue(criteria.matches(createLogEntry(level = LogLevel.ERROR)))
        assertTrue(criteria.matches(createLogEntry(level = LogLevel.WARN)))
        assertFalse(criteria.matches(createLogEntry(level = LogLevel.DEBUG)))
        assertFalse(criteria.matches(createLogEntry(level = LogLevel.INFO)))
        assertFalse(criteria.matches(createLogEntry(level = LogLevel.VERBOSE)))
    }
    
    @Test
    fun `빈 레벨 셋은 모든 로그 차단`() {
        val criteria = FilterCriteria(levels = emptySet())
        
        LogLevel.entries.forEach { level ->
            val entry = createLogEntry(level = level)
            assertFalse(criteria.matches(entry), "Level $level should not match with empty levels")
        }
    }
    
    // 패키지명 필터링 테스트
    
    @Test
    fun `패키지명 부분 일치 필터링`() {
        val criteria = FilterCriteria(packageName = "example")
        
        val matchingEntry = createLogEntry(packageName = "com.example.app")
        val nonMatchingEntry = createLogEntry(packageName = "com.other.app")
        
        assertTrue(criteria.matches(matchingEntry))
        assertFalse(criteria.matches(nonMatchingEntry))
    }
    
    @Test
    fun `패키지명 대소문자 무시 필터링`() {
        val criteria = FilterCriteria(packageName = "EXAMPLE")
        
        val entry = createLogEntry(packageName = "com.example.app")
        
        assertTrue(criteria.matches(entry))
    }
    
    @Test
    fun `패키지명이 없으면 PID로 매칭 시도`() {
        val criteria = FilterCriteria(packageName = "1234")
        
        val entryWithoutPackage = createLogEntry(pid = 1234, packageName = null)
        val entryWithDifferentPid = createLogEntry(pid = 5555, packageName = null)
        
        assertTrue(criteria.matches(entryWithoutPackage))
        assertFalse(criteria.matches(entryWithDifferentPid))
    }
    
    @Test
    fun `빈 패키지명 필터는 모든 로그 통과`() {
        val criteria = FilterCriteria(packageName = "")
        
        val entry1 = createLogEntry(packageName = "com.example.app")
        val entry2 = createLogEntry(packageName = null)
        
        assertTrue(criteria.matches(entry1))
        assertTrue(criteria.matches(entry2))
    }
    
    // PID 필터링 테스트
    
    @Test
    fun `PID 정확히 일치시 통과`() {
        val criteria = FilterCriteria(pid = "1234")
        
        val matchingEntry = createLogEntry(pid = 1234)
        val nonMatchingEntry = createLogEntry(pid = 5678)
        
        assertTrue(criteria.matches(matchingEntry))
        assertFalse(criteria.matches(nonMatchingEntry))
    }
    
    @Test
    fun `유효하지 않은 PID 문자열은 필터링 안함`() {
        val criteria = FilterCriteria(pid = "invalid")
        
        val entry = createLogEntry(pid = 1234)
        
        // 숫자가 아닌 PID는 필터링하지 않음
        assertTrue(criteria.matches(entry))
    }
    
    @Test
    fun `빈 PID 필터는 모든 로그 통과`() {
        val criteria = FilterCriteria(pid = "")
        
        val entry = createLogEntry(pid = 9999)
        
        assertTrue(criteria.matches(entry))
    }
    
    // 태그 필터링 테스트
    
    @Test
    fun `태그 부분 일치 필터링`() {
        val criteria = FilterCriteria(tag = "Activity")
        
        val matchingEntry = createLogEntry(tag = "MainActivity")
        val nonMatchingEntry = createLogEntry(tag = "NetworkService")
        
        assertTrue(criteria.matches(matchingEntry))
        assertFalse(criteria.matches(nonMatchingEntry))
    }
    
    @Test
    fun `태그 대소문자 무시 필터링`() {
        val criteria = FilterCriteria(tag = "MAIN")
        
        val entry = createLogEntry(tag = "MainActivity")
        
        assertTrue(criteria.matches(entry))
    }
    
    @Test
    fun `빈 태그 필터는 모든 로그 통과`() {
        val criteria = FilterCriteria(tag = "")
        
        val entry = createLogEntry(tag = "AnyTag")
        
        assertTrue(criteria.matches(entry))
    }
    
    // 키워드 필터링 테스트
    
    @Test
    fun `메시지 키워드 필터링`() {
        val criteria = FilterCriteria(keyword = "error")
        
        val matchingEntry = createLogEntry(message = "An error occurred")
        val nonMatchingEntry = createLogEntry(message = "Everything is fine")
        
        assertTrue(criteria.matches(matchingEntry))
        assertFalse(criteria.matches(nonMatchingEntry))
    }
    
    @Test
    fun `태그에서도 키워드 검색`() {
        val criteria = FilterCriteria(keyword = "Network")
        
        val matchByTag = createLogEntry(tag = "NetworkManager", message = "Connected")
        val matchByMessage = createLogEntry(tag = "Other", message = "Network error")
        val noMatch = createLogEntry(tag = "Other", message = "Other message")
        
        assertTrue(criteria.matches(matchByTag))
        assertTrue(criteria.matches(matchByMessage))
        assertFalse(criteria.matches(noMatch))
    }
    
    @Test
    fun `키워드 대소문자 무시 필터링`() {
        val criteria = FilterCriteria(keyword = "ERROR")
        
        val entry = createLogEntry(message = "An error occurred")
        
        assertTrue(criteria.matches(entry))
    }
    
    @Test
    fun `빈 키워드 필터는 모든 로그 통과`() {
        val criteria = FilterCriteria(keyword = "")
        
        val entry = createLogEntry(message = "Any message")
        
        assertTrue(criteria.matches(entry))
    }
    
    // 복합 필터링 테스트
    
    @Test
    fun `여러 필터 조합시 모든 조건 만족해야 통과`() {
        val criteria = FilterCriteria(
            levels = setOf(LogLevel.ERROR),
            tag = "Network",
            keyword = "timeout"
        )
        
        // 모든 조건 만족
        val matchingEntry = createLogEntry(
            level = LogLevel.ERROR,
            tag = "NetworkManager",
            message = "Connection timeout"
        )
        assertTrue(criteria.matches(matchingEntry))
        
        // 레벨 불일치
        val wrongLevel = createLogEntry(
            level = LogLevel.DEBUG,
            tag = "NetworkManager",
            message = "Connection timeout"
        )
        assertFalse(criteria.matches(wrongLevel))
        
        // 태그 불일치
        val wrongTag = createLogEntry(
            level = LogLevel.ERROR,
            tag = "Database",
            message = "Connection timeout"
        )
        assertFalse(criteria.matches(wrongTag))
        
        // 키워드 불일치
        val wrongKeyword = createLogEntry(
            level = LogLevel.ERROR,
            tag = "NetworkManager",
            message = "Connected successfully"
        )
        assertFalse(criteria.matches(wrongKeyword))
    }
    
    @Test
    fun `기본 FilterCriteria는 모든 로그 통과`() {
        val criteria = FilterCriteria()
        
        val entry = createLogEntry(
            level = LogLevel.VERBOSE,
            tag = "AnyTag",
            message = "Any message",
            packageName = "any.package"
        )
        
        assertTrue(criteria.matches(entry))
    }
}
