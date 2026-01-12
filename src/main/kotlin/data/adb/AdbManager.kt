package data.adb

import java.io.File

/**
 * ADB 실행 파일 경로를 탐지하고 관리합니다.
 */
object AdbManager {
    
    private var cachedAdbPath: String? = null
    
    /**
     * ADB 실행 파일 경로를 찾습니다.
     * 우선순위: ANDROID_HOME > PATH > 기본 위치
     */
    fun findAdbPath(): String? {
        cachedAdbPath?.let { return it }
        
        val adbPath = findAdbFromAndroidHome()
            ?: findAdbFromPath()
            ?: findAdbFromDefaultLocations()
        
        cachedAdbPath = adbPath
        return adbPath
    }
    
    /**
     * ADB가 사용 가능한지 확인합니다.
     */
    fun isAdbAvailable(): Boolean {
        val adbPath = findAdbPath() ?: return false
        return try {
            val process = ProcessBuilder(adbPath, "version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * ADB 버전을 반환합니다.
     */
    fun getAdbVersion(): String? {
        val adbPath = findAdbPath() ?: return null
        return try {
            val process = ProcessBuilder(adbPath, "version")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readLine()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun findAdbFromAndroidHome(): String? {
        val androidHome = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: return null
        
        val adbFile = File(androidHome, "platform-tools${File.separator}adb${getExecutableExtension()}")
        return if (adbFile.exists() && adbFile.canExecute()) adbFile.absolutePath else null
    }
    
    private fun findAdbFromPath(): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = if (isWindows()) ";" else ":"
        
        for (dir in pathEnv.split(separator)) {
            val adbFile = File(dir, "adb${getExecutableExtension()}")
            if (adbFile.exists() && adbFile.canExecute()) {
                return adbFile.absolutePath
            }
        }
        return null
    }
    
    private fun findAdbFromDefaultLocations(): String? {
        val defaultPaths = if (isWindows()) {
            listOf(
                "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk\\platform-tools\\adb.exe",
                "${System.getenv("USERPROFILE")}\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe"
            )
        } else if (isMac()) {
            listOf(
                "${System.getProperty("user.home")}/Library/Android/sdk/platform-tools/adb",
                "/usr/local/bin/adb",
                "/opt/homebrew/bin/adb"
            )
        } else {
            listOf(
                "${System.getProperty("user.home")}/Android/Sdk/platform-tools/adb",
                "/usr/bin/adb",
                "/usr/local/bin/adb"
            )
        }
        
        for (path in defaultPaths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return file.absolutePath
            }
        }
        return null
    }
    
    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("windows")
    private fun isMac(): Boolean = System.getProperty("os.name").lowercase().contains("mac")
    private fun getExecutableExtension(): String = if (isWindows()) ".exe" else ""
}
