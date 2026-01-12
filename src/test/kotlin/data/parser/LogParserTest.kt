package data.parser

import domain.model.LogLevel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LogParserTest {
    
    private lateinit var parser: LogParser
    
    @BeforeEach
    fun setup() {
        parser = LogParser()
    }
    
    @Test
    fun `정상적인 threadtime 포맷 파싱`() {
        // Given
        val line = "01-10 14:23:45.123  1234  5678 D TestTag : This is a test message"
        
        // When
        val entry = parser.parse(line)
        
        // Then
        assertNotNull(entry)
        assertEquals(1234, entry.pid)
        assertEquals(5678, entry.tid)
        assertEquals(LogLevel.DEBUG, entry.level)
        assertEquals("TestTag", entry.tag)
        assertEquals("This is a test message", entry.message)
        assertEquals(14, entry.timestamp.hour)
        assertEquals(23, entry.timestamp.minute)
        assertEquals(45, entry.timestamp.second)
    }
    
    @Test
    fun `각 로그 레벨 파싱`() {
        val levels = mapOf(
            'V' to LogLevel.VERBOSE,
            'D' to LogLevel.DEBUG,
            'I' to LogLevel.INFO,
            'W' to LogLevel.WARN,
            'E' to LogLevel.ERROR,
            'F' to LogLevel.FATAL
        )
        
        levels.forEach { (char, expectedLevel) ->
            val line = "01-10 14:23:45.123  1234  5678 $char TestTag : Message"
            val entry = parser.parse(line)
            
            assertNotNull(entry, "Entry should not be null for level $char")
            assertEquals(expectedLevel, entry.level, "Level should be $expectedLevel for char $char")
        }
    }
    
    @Test
    fun `빈 줄 파싱시 null 반환`() {
        // Given
        val emptyLine = ""
        val blankLine = "   "
        
        // When & Then
        assertNull(parser.parse(emptyLine))
        assertNull(parser.parse(blankLine))
    }
    
    @Test
    fun `시스템 구분선 파싱시 null 반환`() {
        // Given
        val systemLine = "--------- beginning of main"
        
        // When & Then
        assertNull(parser.parse(systemLine))
    }
    
    @Test
    fun `파싱 불가능한 라인은 fallback 엔트리 생성`() {
        // Given
        val invalidLine = "Some random text without proper format"
        
        // When
        val entry = parser.parse(invalidLine)
        
        // Then
        assertNotNull(entry)
        assertEquals("System", entry.tag)
        assertEquals(invalidLine, entry.message)
        assertEquals(0, entry.pid)
        assertEquals(0, entry.tid)
        assertEquals(LogLevel.VERBOSE, entry.level)
    }
    
    @Test
    fun `ID 카운터가 증가함`() {
        // Given
        val line1 = "01-10 14:23:45.123  1234  5678 D Tag1 : Message 1"
        val line2 = "01-10 14:23:46.123  1234  5678 D Tag2 : Message 2"
        
        // When
        val entry1 = parser.parse(line1)
        val entry2 = parser.parse(line2)
        
        // Then
        assertNotNull(entry1)
        assertNotNull(entry2)
        assertEquals(1, entry1.id)
        assertEquals(2, entry2.id)
    }
    
    @Test
    fun `reset 호출시 ID 카운터 초기화`() {
        // Given
        parser.parse("01-10 14:23:45.123  1234  5678 D Tag : Message")
        parser.parse("01-10 14:23:45.123  1234  5678 D Tag : Message")
        
        // When
        parser.reset()
        val entry = parser.parse("01-10 14:23:45.123  1234  5678 D Tag : Message")
        
        // Then
        assertNotNull(entry)
        assertEquals(1, entry.id)
    }
    
    @Test
    fun `콜론이 포함된 메시지 파싱`() {
        // Given
        val line = "01-10 14:23:45.123  1234  5678 D TestTag : URL: https://example.com:8080/path"
        
        // When
        val entry = parser.parse(line)
        
        // Then
        assertNotNull(entry)
        assertEquals("URL: https://example.com:8080/path", entry.message)
    }
    
    @Test
    fun `긴 태그명 파싱`() {
        // Given
        val longTag = "VeryLongTagNameForTesting"
        val line = "01-10 14:23:45.123  1234  5678 D $longTag : Message"
        
        // When
        val entry = parser.parse(line)
        
        // Then
        assertNotNull(entry)
        assertEquals(longTag, entry.tag)
    }
    
    @Test
    fun `공백이 포함된 메시지 파싱`() {
        // Given
        val line = "01-10 14:23:45.123  1234  5678 D Tag : Message with   multiple   spaces"
        
        // When
        val entry = parser.parse(line)
        
        // Then
        assertNotNull(entry)
        // 정규식 : 뒤 공백은 트리밍됨
        assertEquals("Message with   multiple   spaces", entry.message)
    }
    
    @Test
    fun `실제 adb logcat 출력 파싱 - 공백 패딩된 태그`() {
        // Given - 실제 adb logcat >> file 출력
        val lines = listOf(
            "01-12 00:17:20.633 32069 15416 I FA      : Application backgrounded at: timestamp_millis: 1768205838630",
            "01-12 00:17:20.637 29938 30429 E FrameEvents: updateAcquireFence: Did not find frame.",
            "01-12 00:17:20.663 15535 15561 D ONNX    : Input names [input_ids, attention_mask, token_type_ids] / [last_hidden_state]",
            "01-12 00:17:20.666 15535 15555 D VectorDB: deleteAll() customTarget:com.android.car.radio / null"
        )
        
        // When & Then
        lines.forEachIndexed { index, line ->
            val entry = parser.parse(line)
            assertNotNull(entry, "Line $index should be parsed: $line")
        }
    }
    
    @Test
    fun `실제 adb logcat 출력 파싱 - 필드 값 검증`() {
        // Given
        val line = "01-12 00:17:20.633 32069 15416 I FA      : Application backgrounded at: timestamp_millis: 1768205838630"
        
        // When
        val entry = parser.parse(line)
        
        // Then
        assertNotNull(entry)
        assertEquals(32069, entry.pid)
        assertEquals(15416, entry.tid)
        assertEquals(LogLevel.INFO, entry.level)
        assertEquals("FA", entry.tag)
        assertEquals("Application backgrounded at: timestamp_millis: 1768205838630", entry.message)
    }
}
