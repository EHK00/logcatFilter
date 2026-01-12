package ui.viewmodel

import androidx.compose.ui.unit.dp
import domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MainViewModel 상태 관리 테스트
 * 
 * Note: ADB 관련 기능은 실제 디바이스/에뮬레이터가 필요하므로
 * 순수 상태 관리 기능만 테스트합니다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    
    private lateinit var viewModel: MainViewModel
    
    @BeforeEach
    fun setup() {
        viewModel = MainViewModel()
    }
    
    @AfterEach
    fun tearDown() {
        viewModel.dispose()
    }
    
    // 초기 상태 테스트
    
    @Test
    fun `초기 상태 확인`() = runTest {
        // UI State
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isCapturing)
        assertFalse(uiState.isLoading)
        
        // 선택된 디바이스 없음
        assertEquals(null, viewModel.selectedDevice.value)
        
        // 필터 쿼리 기본값
        assertEquals("", viewModel.filterQuery.value)
        
        // 자동 스크롤 활성화
        assertTrue(viewModel.autoScrollEnabled.value)
        
        // 빈 로그
        assertTrue(viewModel.allLogs.value.isEmpty())
        assertTrue(viewModel.filteredLogs.value.isEmpty())
    }
    
    // 필터 쿼리 테스트
    
    @Test
    fun `필터 쿼리 업데이트`() = runTest {
        // When
        viewModel.updateFilterQuery("package:com.example")
        
        // Then
        assertEquals("package:com.example", viewModel.filterQuery.value)
    }
    
    @Test
    fun `필터 쿼리 클리어`() = runTest {
        // Given
        viewModel.updateFilterQuery("package:com.example")
        
        // When
        viewModel.updateFilterQuery("")
        
        // Then
        assertEquals("", viewModel.filterQuery.value)
    }
    
    // 컬럼 설정 테스트
    
    @Test
    fun `컬럼 너비 업데이트`() = runTest {
        // When
        viewModel.updateColumnWidth(LogColumn.TAG, 200.dp)
        
        // Then
        assertEquals(200.dp, viewModel.columnConfig.value.getWidth(LogColumn.TAG))
    }
    
    @Test
    fun `컬럼 표시 설정`() = runTest {
        // When
        viewModel.setColumnVisibility(LogColumn.LEVEL, false)
        
        // Then
        assertFalse(viewModel.columnConfig.value.isVisible(LogColumn.LEVEL))
        
        // When
        viewModel.setColumnVisibility(LogColumn.LEVEL, true)
        
        // Then
        assertTrue(viewModel.columnConfig.value.isVisible(LogColumn.LEVEL))
    }
    
    // 자동 스크롤 테스트
    
    @Test
    fun `자동 스크롤 토글`() = runTest {
        // Given
        assertTrue(viewModel.autoScrollEnabled.value)
        
        // When
        viewModel.toggleAutoScroll()
        
        // Then
        assertFalse(viewModel.autoScrollEnabled.value)
        
        // When
        viewModel.toggleAutoScroll()
        
        // Then
        assertTrue(viewModel.autoScrollEnabled.value)
    }
    
    @Test
    fun `자동 스크롤 설정`() = runTest {
        // When
        viewModel.setAutoScroll(false)
        
        // Then
        assertFalse(viewModel.autoScrollEnabled.value)
        
        // When
        viewModel.setAutoScroll(true)
        
        // Then
        assertTrue(viewModel.autoScrollEnabled.value)
    }
    
    // 에러 메시지 테스트
    
    @Test
    fun `에러 메시지 클리어`() = runTest {
        // When
        viewModel.clearError()
        
        // Then
        assertEquals(null, viewModel.errorMessage.value)
    }
    
    // 디바이스 선택 테스트
    
    @Test
    fun `디바이스 선택`() = runTest {
        // Given
        val device = Device(
            id = "emulator-5554",
            name = "Pixel_4_API_30",
            state = DeviceState.ONLINE,
            type = DeviceType.EMULATOR
        )
        
        // When
        viewModel.selectDevice(device)
        
        // Then
        assertEquals(device, viewModel.selectedDevice.value)
    }
    
    @Test
    fun `디바이스 선택 해제`() = runTest {
        // Given
        val device = Device(
            id = "emulator-5554",
            name = "Pixel_4_API_30",
            state = DeviceState.ONLINE,
            type = DeviceType.EMULATOR
        )
        viewModel.selectDevice(device)
        
        // When
        viewModel.selectDevice(null)
        
        // Then
        assertEquals(null, viewModel.selectedDevice.value)
    }
    
    // 로그 클리어 테스트
    
    @Test
    fun `로그 클리어`() = runTest {
        // When
        viewModel.clearLogs()
        
        // Then
        assertTrue(viewModel.allLogs.value.isEmpty())
        assertTrue(viewModel.filteredLogs.value.isEmpty())
    }
    
    // 검색 기능 테스트 (필터와 별도)
    
    @Test
    fun `검색어 초기 상태`() = runTest {
        // Then
        assertEquals("", viewModel.searchQuery.value)
    }
    
    @Test
    fun `검색어 업데이트`() = runTest {
        // When
        viewModel.updateSearchQuery("error")
        
        // Then
        assertEquals("error", viewModel.searchQuery.value)
    }
    
    @Test
    fun `검색어 클리어`() = runTest {
        // Given
        viewModel.updateSearchQuery("error")
        
        // When
        viewModel.clearSearch()
        
        // Then
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(0, viewModel.currentSearchIndex.value)
    }
    
    @Test
    fun `검색 인덱스 초기 상태`() = runTest {
        // Then
        assertEquals(0, viewModel.currentSearchIndex.value)
        assertEquals(0, viewModel.searchMatchCount.value)
    }
}
