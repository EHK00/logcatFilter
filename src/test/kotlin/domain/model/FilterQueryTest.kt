package domain.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterQueryTest {
    
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
    
    
    // 파싱 테스트
    
    @Test
    fun `빈 쿼리 파싱`() {
        val query = FilterQuery.parse("")
        assertTrue(query.conditions.isEmpty())
        assertEquals("", query.plainText)
    }
    
    @Test
    fun `일반 텍스트 쿼리 파싱`() {
        val query = FilterQuery.parse("error message")
        assertTrue(query.conditions.isEmpty())
        assertEquals("error message", query.plainText)
    }
    
    @Test
    fun `package 키워드 파싱`() {
        val query = FilterQuery.parse("package:com.example")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.PACKAGE, query.conditions[0].type)
        assertEquals("com.example", query.conditions[0].value)
        assertEquals(MatchType.CONTAINS, query.conditions[0].matchType)
        assertFalse(query.conditions[0].negate)
    }
    
    @Test
    fun `package= 정확히 일치 파싱`() {
        val query = FilterQuery.parse("package=:com.example.app")
        assertEquals(1, query.conditions.size)
        assertEquals(MatchType.EXACT, query.conditions[0].matchType)
    }
    
    @Test
    fun `package~ 정규식 파싱`() {
        val query = FilterQuery.parse("package~:com\\.example\\..*")
        assertEquals(1, query.conditions.size)
        assertEquals(MatchType.REGEX, query.conditions[0].matchType)
    }
    
    @Test
    fun `-package 부정 파싱`() {
        val query = FilterQuery.parse("-package:excluded")
        assertEquals(1, query.conditions.size)
        assertTrue(query.conditions[0].negate)
    }
    
    @Test
    fun `tag 키워드 파싱`() {
        val query = FilterQuery.parse("tag:MainActivity")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.TAG, query.conditions[0].type)
        assertEquals("MainActivity", query.conditions[0].value)
    }
    
    @Test
    fun `level 키워드 파싱`() {
        val query = FilterQuery.parse("level:error")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.LEVEL, query.conditions[0].type)
        assertEquals("error", query.conditions[0].value)
    }
    
    @Test
    fun `pid 키워드 파싱`() {
        val query = FilterQuery.parse("pid:1234")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.PID, query.conditions[0].type)
        assertEquals("1234", query.conditions[0].value)
    }
    
    @Test
    fun `message 키워드 파싱`() {
        val query = FilterQuery.parse("message:exception")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.MESSAGE, query.conditions[0].type)
    }
    
    @Test
    fun `여러 조건 파싱`() {
        val query = FilterQuery.parse("package:com.example tag:Main level:error")
        assertEquals(3, query.conditions.size)
    }
    
    @Test
    fun `키워드와 일반 텍스트 혼합 파싱`() {
        val query = FilterQuery.parse("package:com.example some text")
        assertEquals(1, query.conditions.size)
        assertEquals("some text", query.plainText.trim())
    }
    
    @Test
    fun `pkg 축약어 파싱`() {
        val query = FilterQuery.parse("pkg:com.example")
        assertEquals(ConditionType.PACKAGE, query.conditions[0].type)
    }
    
    @Test
    fun `process 키워드 파싱`() {
        val query = FilterQuery.parse("process:com.example")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.PACKAGE, query.conditions[0].type)
        assertEquals("com.example", query.conditions[0].value)
    }
    
    @Test
    fun `-process 부정 파싱`() {
        val query = FilterQuery.parse("-process:excluded")
        assertEquals(1, query.conditions.size)
        assertEquals(ConditionType.PACKAGE, query.conditions[0].type)
        assertTrue(query.conditions[0].negate)
    }
    
    @Test
    fun `msg 축약어 파싱`() {
        val query = FilterQuery.parse("msg:error")
        assertEquals(ConditionType.MESSAGE, query.conditions[0].type)
    }
    
    @Test
    fun `lvl 축약어 파싱`() {
        val query = FilterQuery.parse("lvl:warn")
        assertEquals(ConditionType.LEVEL, query.conditions[0].type)
    }
    
    // 매칭 테스트
    
    @Test
    fun `package 부분 일치 매칭`() {
        val query = FilterQuery.parse("package:example")
        
        val matchingEntry = createLogEntry(packageName = "com.example.app")
        val nonMatchingEntry = createLogEntry(packageName = "com.other.app")
        
        assertTrue(query.matches(matchingEntry))
        assertFalse(query.matches(nonMatchingEntry))
    }
    
    @Test
    fun `package 정확히 일치 매칭`() {
        val query = FilterQuery.parse("package=:com.example.app")
        
        val exactMatch = createLogEntry(packageName = "com.example.app")
        val partialMatch = createLogEntry(packageName = "com.example.app.sub")
        
        assertTrue(query.matches(exactMatch))
        assertFalse(query.matches(partialMatch))
    }
    
    @Test
    fun `package 정규식 매칭`() {
        val query = FilterQuery.parse("package~:com\\.example\\..*")
        
        val matchingEntry = createLogEntry(packageName = "com.example.app")
        val nonMatchingEntry = createLogEntry(packageName = "com.other.app")
        
        assertTrue(query.matches(matchingEntry))
        assertFalse(query.matches(nonMatchingEntry))
    }
    
    @Test
    fun `-package 제외 매칭`() {
        val query = FilterQuery.parse("-package:excluded")
        
        val excludedEntry = createLogEntry(packageName = "com.excluded.app")
        val includedEntry = createLogEntry(packageName = "com.included.app")
        
        assertFalse(query.matches(excludedEntry))
        assertTrue(query.matches(includedEntry))
    }
    
    @Test
    fun `tag 매칭`() {
        val query = FilterQuery.parse("tag:Activity")
        
        val matchingEntry = createLogEntry(tag = "MainActivity")
        val nonMatchingEntry = createLogEntry(tag = "NetworkService")
        
        assertTrue(query.matches(matchingEntry))
        assertFalse(query.matches(nonMatchingEntry))
    }
    
    @Test
    fun `level 매칭 - 해당 레벨 이상`() {
        val query = FilterQuery.parse("level:warn")
        
        val warnEntry = createLogEntry(level = LogLevel.WARN)
        val errorEntry = createLogEntry(level = LogLevel.ERROR)
        val debugEntry = createLogEntry(level = LogLevel.DEBUG)
        
        assertTrue(query.matches(warnEntry))
        assertTrue(query.matches(errorEntry))
        assertFalse(query.matches(debugEntry))
    }
    
    @Test
    fun `level= 정확히 일치`() {
        val query = FilterQuery.parse("level=:error")
        
        val errorEntry = createLogEntry(level = LogLevel.ERROR)
        val warnEntry = createLogEntry(level = LogLevel.WARN)
        
        assertTrue(query.matches(errorEntry))
        assertFalse(query.matches(warnEntry))
    }
    
    @Test
    fun `message 매칭`() {
        val query = FilterQuery.parse("message:exception")
        
        val matchingEntry = createLogEntry(message = "NullPointerException occurred")
        val nonMatchingEntry = createLogEntry(message = "Everything is fine")
        
        assertTrue(query.matches(matchingEntry))
        assertFalse(query.matches(nonMatchingEntry))
    }
    
    @Test
    fun `일반 텍스트 검색 - message, tag, packageName에서 검색`() {
        val query = FilterQuery.parse("error")
        
        val matchByMessage = createLogEntry(message = "An error occurred")
        val matchByTag = createLogEntry(tag = "ErrorHandler")
        val matchByPackage = createLogEntry(packageName = "com.error.handler")
        val noMatch = createLogEntry(tag = "Success", message = "All good")
        
        assertTrue(query.matches(matchByMessage))
        assertTrue(query.matches(matchByTag))
        assertTrue(query.matches(matchByPackage))
        assertFalse(query.matches(noMatch))
    }
    
    @Test
    fun `여러 조건 AND 매칭`() {
        val query = FilterQuery.parse("package:example tag:Main")
        
        val bothMatch = createLogEntry(packageName = "com.example.app", tag = "MainActivity")
        val packageOnly = createLogEntry(packageName = "com.example.app", tag = "Other")
        val tagOnly = createLogEntry(packageName = "com.other.app", tag = "MainActivity")
        
        assertTrue(query.matches(bothMatch))
        assertFalse(query.matches(packageOnly))
        assertFalse(query.matches(tagOnly))
    }
    
    @Test
    fun `대소문자 무시 매칭`() {
        val query = FilterQuery.parse("package:EXAMPLE tag:MAIN")
        
        val entry = createLogEntry(packageName = "com.example.app", tag = "MainActivity")
        
        assertTrue(query.matches(entry))
    }
    
    @Test
    fun `패키지명 없으면 PID로 매칭`() {
        val query = FilterQuery.parse("package:1234")
        
        val entry = createLogEntry(pid = 1234, packageName = null)
        
        assertTrue(query.matches(entry))
    }
    
    @Test
    fun `level 다양한 형식 지원`() {
        // 짧은 형식
        assertTrue(FilterQuery.parse("level:e").matches(createLogEntry(level = LogLevel.ERROR)))
        assertTrue(FilterQuery.parse("level:w").matches(createLogEntry(level = LogLevel.WARN)))
        assertTrue(FilterQuery.parse("level:i").matches(createLogEntry(level = LogLevel.INFO)))
        assertTrue(FilterQuery.parse("level:d").matches(createLogEntry(level = LogLevel.DEBUG)))
        assertTrue(FilterQuery.parse("level:v").matches(createLogEntry(level = LogLevel.VERBOSE)))
        
        // 긴 형식
        assertTrue(FilterQuery.parse("level:error").matches(createLogEntry(level = LogLevel.ERROR)))
        assertTrue(FilterQuery.parse("level:warning").matches(createLogEntry(level = LogLevel.WARN)))
    }
    
    // OR 연산 테스트
    
    @Test
    fun `동일 키 여러개는 OR 연산`() {
        val query = FilterQuery.parse("package:interactor package:phone")
        
        val matchFirst = createLogEntry(packageName = "com.interactor.app")
        val matchSecond = createLogEntry(packageName = "com.phone.app")
        val matchNeither = createLogEntry(packageName = "com.other.app")
        
        assertTrue(query.matches(matchFirst))
        assertTrue(query.matches(matchSecond))
        assertFalse(query.matches(matchNeither))
    }
    
    @Test
    fun `동일 키 3개 이상도 OR 연산`() {
        val query = FilterQuery.parse("tag:Activity tag:Service tag:Fragment")
        
        val matchActivity = createLogEntry(tag = "MainActivity")
        val matchService = createLogEntry(tag = "NetworkService")
        val matchFragment = createLogEntry(tag = "HomeFragment")
        val matchNone = createLogEntry(tag = "OtherClass")
        
        assertTrue(query.matches(matchActivity))
        assertTrue(query.matches(matchService))
        assertTrue(query.matches(matchFragment))
        assertFalse(query.matches(matchNone))
    }
    
    @Test
    fun `다른 키 조합은 AND 연산`() {
        // (package:foo OR package:bar) AND tag:Main
        val query = FilterQuery.parse("package:foo package:bar tag:Main")
        
        val fooWithMain = createLogEntry(packageName = "com.foo.app", tag = "MainActivity")
        val barWithMain = createLogEntry(packageName = "com.bar.app", tag = "MainActivity")
        val fooWithOther = createLogEntry(packageName = "com.foo.app", tag = "OtherClass")
        val otherWithMain = createLogEntry(packageName = "com.other.app", tag = "MainActivity")
        
        assertTrue(query.matches(fooWithMain))
        assertTrue(query.matches(barWithMain))
        assertFalse(query.matches(fooWithOther))  // tag 불일치
        assertFalse(query.matches(otherWithMain))  // package 불일치
    }
    
    @Test
    fun `부정 조건과 OR 조합`() {
        // (package:foo OR package:bar) AND NOT excluded
        val query = FilterQuery.parse("package:foo package:bar -package:excluded")
        
        val fooOk = createLogEntry(packageName = "com.foo.app")
        val barOk = createLogEntry(packageName = "com.bar.app")
        val fooExcluded = createLogEntry(packageName = "com.foo.excluded")
        val other = createLogEntry(packageName = "com.other.app")
        
        assertTrue(query.matches(fooOk))
        assertTrue(query.matches(barOk))
        assertFalse(query.matches(fooExcluded))  // excluded 포함
        assertFalse(query.matches(other))  // foo/bar 불일치
    }
    
    @Test
    fun `message OR 연산`() {
        val query = FilterQuery.parse("message:error message:exception")
        
        val errorMsg = createLogEntry(message = "An error occurred")
        val exceptionMsg = createLogEntry(message = "NullPointerException thrown")
        val successMsg = createLogEntry(message = "Operation successful")
        
        assertTrue(query.matches(errorMsg))
        assertTrue(query.matches(exceptionMsg))
        assertFalse(query.matches(successMsg))
    }
    
    // 하이라이트 검색어 추출 테스트
    
    @Test
    fun `일반 텍스트에서 하이라이트 검색어 추출`() {
        val query = FilterQuery.parse("error")
        val terms = query.getHighlightTerms()
        
        assertEquals(listOf("error"), terms)
    }
    
    @Test
    fun `message 키워드에서 하이라이트 검색어 추출`() {
        val query = FilterQuery.parse("message:exception")
        val terms = query.getHighlightTerms()
        
        assertEquals(listOf("exception"), terms)
    }
    
    @Test
    fun `여러 message 키워드에서 하이라이트 검색어 추출`() {
        val query = FilterQuery.parse("message:error message:exception")
        val terms = query.getHighlightTerms()
        
        assertEquals(listOf("error", "exception"), terms)
    }
    
    @Test
    fun `일반 텍스트와 message 키워드 혼합 하이라이트 추출`() {
        val query = FilterQuery.parse("message:exception fatal")
        val terms = query.getHighlightTerms()
        
        assertTrue(terms.contains("exception"))
        assertTrue(terms.contains("fatal"))
    }
    
    @Test
    fun `package, tag 키워드는 하이라이트 대상 아님`() {
        val query = FilterQuery.parse("package:example tag:Main")
        val terms = query.getHighlightTerms()
        
        assertTrue(terms.isEmpty())
    }
    
    @Test
    fun `부정 message 키워드는 하이라이트 대상 아님`() {
        val query = FilterQuery.parse("-message:excluded")
        val terms = query.getHighlightTerms()
        
        assertTrue(terms.isEmpty())
    }
    
    @Test
    fun `빈 쿼리는 빈 하이라이트 목록`() {
        val query = FilterQuery.parse("")
        val terms = query.getHighlightTerms()
        
        assertTrue(terms.isEmpty())
    }
    
    @Test
    fun `복합 쿼리에서 하이라이트 검색어만 추출`() {
        val query = FilterQuery.parse("package:com.example tag:Main message:crash error text")
        val terms = query.getHighlightTerms()
        
        // message:crash와 plainText(error text) 만 포함
        assertTrue(terms.contains("crash"))
        assertTrue(terms.contains("error text"))
        // package, tag 값은 포함하지 않음
        assertFalse(terms.contains("com.example"))
        assertFalse(terms.contains("Main"))
    }
}
