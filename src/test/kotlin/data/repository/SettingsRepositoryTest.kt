package data.repository

import domain.model.AppSettings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsRepositoryTest {

    private lateinit var testDir: File
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setup() {
        testDir = File(System.getProperty("java.io.tmpdir"), "logcatfilter_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
        repository = SettingsRepository(testDir)
    }

    @AfterEach
    fun teardown() {
        testDir.deleteRecursively()
    }

    @Test
    fun `기본 설정값을 반환한다`() {
        // Given: 설정 파일이 없는 상태
        
        // When
        val settings = repository.load()
        
        // Then
        assertEquals(AppSettings.DEFAULT_LOG_SAVE_DIR, settings.logSaveDirectory)
        assertTrue(settings.autoSaveOnStop)
    }

    @Test
    fun `설정을 저장하고 불러올 수 있다`() {
        // Given
        val customSettings = AppSettings(
            logSaveDirectory = "/custom/path/logs",
            autoSaveOnStop = false
        )
        
        // When
        repository.save(customSettings)
        val loaded = repository.load()
        
        // Then
        assertEquals("/custom/path/logs", loaded.logSaveDirectory)
        assertEquals(false, loaded.autoSaveOnStop)
    }

    @Test
    fun `설정 파일이 손상되면 기본값을 반환한다`() {
        // Given: 손상된 설정 파일
        val settingsFile = File(testDir, "settings.json")
        settingsFile.writeText("{ invalid json }")
        
        // When
        val settings = repository.load()
        
        // Then
        assertEquals(AppSettings.DEFAULT_LOG_SAVE_DIR, settings.logSaveDirectory)
    }

    @Test
    fun `logSaveDirectory 업데이트가 가능하다`() {
        // Given
        val initialSettings = repository.load()
        
        // When
        val updated = initialSettings.copy(logSaveDirectory = "/new/path")
        repository.save(updated)
        val loaded = repository.load()
        
        // Then
        assertEquals("/new/path", loaded.logSaveDirectory)
    }
}
