package domain.model

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColumnConfigTest {
    
    @Test
    fun `기본 ColumnConfig 생성시 모든 컬럼 표시`() {
        // Given
        val config = ColumnConfig()
        
        // Then
        LogColumn.entries.forEach { column ->
            assertTrue(config.isVisible(column), "$column should be visible by default")
        }
    }
    
    @Test
    fun `기본 ColumnConfig 생성시 기본 너비 설정`() {
        // Given
        val config = ColumnConfig()
        
        // Then
        LogColumn.entries.forEach { column ->
            assertEquals(column.defaultWidth, config.getWidth(column), "$column should have default width")
        }
    }
    
    @Test
    fun `컬럼 너비 설정`() {
        // Given
        val config = ColumnConfig()
        val newWidth = 200.dp
        
        // When
        val updatedConfig = config.setWidth(LogColumn.TAG, newWidth)
        
        // Then
        assertEquals(newWidth, updatedConfig.getWidth(LogColumn.TAG))
        // 다른 컬럼은 영향 없음
        assertEquals(LogColumn.TIME.defaultWidth, updatedConfig.getWidth(LogColumn.TIME))
    }
    
    @Test
    fun `컬럼 너비를 minWidth 이하로 설정시 minWidth로 조정`() {
        // Given
        val config = ColumnConfig()
        val tooSmallWidth = 10.dp
        
        // When
        val updatedConfig = config.setWidth(LogColumn.TAG, tooSmallWidth)
        
        // Then
        assertEquals(LogColumn.TAG.minWidth, updatedConfig.getWidth(LogColumn.TAG))
    }
    
    @Test
    fun `컬럼의 표시 토글`() {
        // Given
        val config = ColumnConfig()
        assertTrue(config.isVisible(LogColumn.TID))
        
        // When - 숨기기
        val hiddenConfig = config.toggleVisibility(LogColumn.TID)
        
        // Then
        assertFalse(hiddenConfig.isVisible(LogColumn.TID))
        
        // When - 다시 보이기
        val visibleConfig = hiddenConfig.toggleVisibility(LogColumn.TID)
        
        // Then
        assertTrue(visibleConfig.isVisible(LogColumn.TID))
    }
    
    @Test
    fun `setVisibility로 컬럼 표시 설정`() {
        // Given
        val config = ColumnConfig()
        
        // When - 숨기기
        val hiddenConfig = config.setVisibility(LogColumn.TAG, false)
        
        // Then
        assertFalse(hiddenConfig.isVisible(LogColumn.TAG))
        
        // When - 보이기
        val visibleConfig = hiddenConfig.setVisibility(LogColumn.TAG, true)
        
        // Then
        assertTrue(visibleConfig.isVisible(LogColumn.TAG))
    }
    
    @Test
    fun `visibleColumns는 표시되는 컬럼만 반환`() {
        // Given
        val config = ColumnConfig()
            .setVisibility(LogColumn.TID, false)
            .setVisibility(LogColumn.LEVEL, false)
        
        // When
        val visibleColumns = config.visibleColumns
        
        // Then
        assertEquals(4, visibleColumns.size)
        assertTrue(LogColumn.TIME in visibleColumns)
        assertTrue(LogColumn.PACKAGE in visibleColumns)
        assertTrue(LogColumn.TAG in visibleColumns)
        assertTrue(LogColumn.MESSAGE in visibleColumns)
        assertFalse(LogColumn.TID in visibleColumns)
        assertFalse(LogColumn.LEVEL in visibleColumns)
    }
    
    @Test
    fun `visibleColumns는 columnOrder 순서 유지`() {
        // Given
        val config = ColumnConfig()
        
        // When
        val visibleColumns = config.visibleColumns
        
        // Then - columnOrder 순서와 동일 (기본: LogColumn.DEFAULT_ORDER)
        assertEquals(LogColumn.DEFAULT_ORDER, visibleColumns)
    }
    
    @Test
    fun `여러 설정 변경이 독립적으로 적용`() {
        // Given
        val config = ColumnConfig()
        
        // When
        val updatedConfig = config
            .setWidth(LogColumn.TAG, 200.dp)
            .setVisibility(LogColumn.TID, false)
            .setWidth(LogColumn.PACKAGE, 250.dp)
        
        // Then
        assertEquals(200.dp, updatedConfig.getWidth(LogColumn.TAG))
        assertFalse(updatedConfig.isVisible(LogColumn.TID))
        assertEquals(250.dp, updatedConfig.getWidth(LogColumn.PACKAGE))
        // 변경하지 않은 값은 기본값 유지
        assertTrue(updatedConfig.isVisible(LogColumn.TAG))
        assertEquals(LogColumn.TIME.defaultWidth, updatedConfig.getWidth(LogColumn.TIME))
    }
    
    @Test
    fun `ColumnState 기본값 확인`() {
        // Given
        val state = ColumnState(column = LogColumn.TAG)
        
        // Then
        assertEquals(LogColumn.TAG, state.column)
        assertEquals(LogColumn.TAG.defaultWidth, state.width)
        assertTrue(state.visible)
    }
    
    // ========== 텍스트 기반 설정 테스트 ==========
    
    @Test
    fun `toConfigText는 보이는 컬럼을 텍스트로 변환`() {
        // Given
        val config = ColumnConfig()
        
        // When
        val text = config.toConfigText()
        
        // Then
        assertEquals("{time}{package}{tid}{lvl}{tag}{message}", text)
    }
    
    @Test
    fun `toConfigText는 숨겨진 컬럼 제외`() {
        // Given
        val config = ColumnConfig()
            .setVisibility(LogColumn.TID, false)
            .setVisibility(LogColumn.LEVEL, false)
        
        // When
        val text = config.toConfigText()
        
        // Then
        assertEquals("{time}{package}{tag}{message}", text)
    }
    
    @Test
    fun `fromConfigText는 텍스트를 파싱하여 설정 생성`() {
        // Given
        val text = "{time}{package}{tid}{lvl}{tag}{message}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then
        LogColumn.entries.forEach { column ->
            assertTrue(config.isVisible(column))
        }
    }
    
    @Test
    fun `fromConfigText는 누락된 컬럼을 숨김 처리`() {
        // Given
        val text = "{time}{package}{tag}{message}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then
        assertTrue(config.isVisible(LogColumn.TIME))
        assertTrue(config.isVisible(LogColumn.PACKAGE))
        assertTrue(config.isVisible(LogColumn.TAG))
        assertTrue(config.isVisible(LogColumn.MESSAGE))
        assertFalse(config.isVisible(LogColumn.TID))
        assertFalse(config.isVisible(LogColumn.LEVEL))
    }
    
    @Test
    fun `fromConfigText는 컬럼 순서를 반영`() {
        // Given: 순서 변경된 텍스트
        val text = "{message}{tag}{lvl}{tid}{package}{time}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then: 순서가 변경됨
        val expected = listOf(
            LogColumn.MESSAGE, LogColumn.TAG, LogColumn.LEVEL, 
            LogColumn.TID, LogColumn.PACKAGE, LogColumn.TIME
        )
        assertEquals(expected, config.visibleColumns)
    }
    
    @Test
    fun `fromConfigText는 대소문자 무시`() {
        // Given
        val text = "{TIME}{Package}{TID}{LVL}{Tag}{MESSAGE}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then
        LogColumn.entries.forEach { column ->
            assertTrue(config.isVisible(column))
        }
    }
    
    @Test
    fun `fromConfigText는 잘못된 태그 무시`() {
        // Given
        val text = "{time}{invalid}{package}{unknown}{message}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then: 유효한 컬럼만 표시
        assertTrue(config.isVisible(LogColumn.TIME))
        assertTrue(config.isVisible(LogColumn.PACKAGE))
        assertTrue(config.isVisible(LogColumn.MESSAGE))
        assertFalse(config.isVisible(LogColumn.TID))
        assertFalse(config.isVisible(LogColumn.LEVEL))
        assertFalse(config.isVisible(LogColumn.TAG))
    }
    
    @Test
    fun `fromConfigText는 빈 텍스트면 기본값 반환`() {
        // Given
        val text = ""
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then: 기본 설정
        LogColumn.entries.forEach { column ->
            assertTrue(config.isVisible(column))
        }
    }
    
    @Test
    fun `fromConfigText는 중복 태그 처리`() {
        // Given
        val text = "{time}{time}{package}{package}"
        
        // When
        val config = ColumnConfig.fromConfigText(text)
        
        // Then: 중복 제거되고 첫 번째 위치 유지
        assertTrue(config.isVisible(LogColumn.TIME))
        assertTrue(config.isVisible(LogColumn.PACKAGE))
        assertEquals(2, config.visibleColumns.size)
    }
    
    @Test
    fun `default는 기본 설정 반환`() {
        // When
        val config = ColumnConfig.default()
        
        // Then
        assertEquals(ColumnConfig(), config)
    }
    
    @Test
    fun `LogColumn fromTag 테스트`() {
        assertEquals(LogColumn.TIME, LogColumn.fromTag("time"))
        assertEquals(LogColumn.PACKAGE, LogColumn.fromTag("package"))
        assertEquals(LogColumn.TID, LogColumn.fromTag("tid"))
        assertEquals(LogColumn.LEVEL, LogColumn.fromTag("lvl"))
        assertEquals(LogColumn.TAG, LogColumn.fromTag("tag"))
        assertEquals(LogColumn.MESSAGE, LogColumn.fromTag("message"))
        assertEquals(null, LogColumn.fromTag("invalid"))
    }
}
