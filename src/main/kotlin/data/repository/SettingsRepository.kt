package data.repository

import domain.model.AppSettings
import java.io.File

/**
 * 애플리케이션 설정의 저장/로드를 담당합니다.
 * JSON 형식으로 파일에 저장합니다.
 */
class SettingsRepository(
    private val configDir: File = File(System.getProperty("user.home"), ".logcatfilter")
) {
    private val settingsFile = File(configDir, "settings.json")
    
    /**
     * 설정을 로드합니다.
     * 파일이 없거나 파싱 실패 시 기본값을 반환합니다.
     */
    fun load(): AppSettings {
        if (!settingsFile.exists()) {
            return AppSettings()
        }
        
        return try {
            val content = settingsFile.readText()
            parseJson(content)
        } catch (e: Exception) {
            AppSettings()
        }
    }
    
    /**
     * 설정을 저장합니다.
     */
    fun save(settings: AppSettings) {
        configDir.mkdirs()
        val json = toJson(settings)
        settingsFile.writeText(json)
    }
    
    /**
     * 간단한 JSON 파싱 (외부 라이브러리 없이 구현)
     */
    private fun parseJson(json: String): AppSettings {
        val logSaveDir = extractJsonString(json, "logSaveDirectory") ?: AppSettings.DEFAULT_LOG_SAVE_DIR
        val autoSave = extractJsonBoolean(json, "autoSaveOnStop") ?: true
        
        return AppSettings(
            logSaveDirectory = logSaveDir,
            autoSaveOnStop = autoSave
        )
    }
    
    /**
     * 설정을 JSON 문자열로 변환합니다.
     */
    private fun toJson(settings: AppSettings): String {
        return """
            {
              "logSaveDirectory": "${escapeJson(settings.logSaveDirectory)}",
              "autoSaveOnStop": ${settings.autoSaveOnStop}
            }
        """.trimIndent()
    }
    
    private fun extractJsonString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.let { unescapeJson(it) }
    }
    
    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val regex = """"$key"\s*:\s*(true|false)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }
    
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun unescapeJson(value: String): String {
        return value
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
