package data.adb

import domain.model.Device
import domain.model.DeviceState
import domain.model.DeviceType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.BufferedReader

/**
 * ADB와 통신하여 디바이스 목록 조회 및 Logcat 스트리밍을 제공합니다.
 */
class AdbService {
    
    private val adbPath: String = AdbManager.findAdbPath()
        ?: throw IllegalStateException("ADB를 찾을 수 없습니다. ANDROID_HOME 또는 PATH를 확인하세요.")
    
    private var logcatProcess: Process? = null
    private var logcatJob: Job? = null
    
    /**
     * 연결된 디바이스 목록을 반환합니다.
     */
    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "devices", "-l")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            parseDeviceList(output)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 디바이스 목록을 주기적으로 갱신하는 Flow를 반환합니다.
     */
    fun observeDevices(intervalMs: Long = 3000): Flow<List<Device>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getDevices())
            delay(intervalMs)
        }
    }.distinctUntilChanged()
    
    /**
     * Logcat 스트리밍을 시작합니다.
     */
    fun startLogcat(
        deviceId: String,
        scope: CoroutineScope
    ): Flow<String> = callbackFlow {
        stopLogcat() // 기존 프로세스 종료
        
        val processBuilder = ProcessBuilder(
            adbPath, "-s", deviceId, "logcat", "-v", "threadtime"
        ).redirectErrorStream(true)
        
        logcatProcess = processBuilder.start()
        val reader = logcatProcess!!.inputStream.bufferedReader()
        
        logcatJob = scope.launch(Dispatchers.IO) {
            try {
                reader.useLines { lines ->
                    for (line in lines) {
                        if (!isActive) break
                        trySend(line)
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    // 로그 에러 처리
                }
            } finally {
                close()
            }
        }
        
        awaitClose {
            stopLogcat()
        }
    }.buffer(1000) // 버퍼링으로 백프레셔 처리
    
    /**
     * Logcat 스트리밍을 중지합니다.
     */
    fun stopLogcat() {
        logcatJob?.cancel()
        logcatJob = null
        
        logcatProcess?.let { process ->
            try {
                process.destroyForcibly()
            } catch (e: Exception) {
                // ignore
            }
        }
        logcatProcess = null
    }
    
    /**
     * Logcat 버퍼를 클리어합니다.
     */
    suspend fun clearLogcat(deviceId: String) = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "-s", deviceId, "logcat", "-c")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
        } catch (e: Exception) {
            // ignore
        }
    }
    
    /**
     * 특정 패키지의 PID를 조회합니다.
     */
    suspend fun getPidForPackage(deviceId: String, packageName: String): Int? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "-s", deviceId, "shell", "pidof", packageName)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            
            output.split("\\s+".toRegex()).firstOrNull()?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 실행 중인 모든 프로세스의 PID-패키지명 매핑을 조회합니다.
     * `adb shell ps -A -o PID,NAME` 명령을 사용합니다.
     */
    suspend fun getProcessMap(deviceId: String): Map<Int, String> = withContext(Dispatchers.IO) {
        try {
            // 방법 1: ps 명령어 사용
            val process = ProcessBuilder(
                adbPath, "-s", deviceId, "shell", 
                "ps", "-A", "-o", "PID,NAME"
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            parseProcessList(output).ifEmpty {
                // 방법 2: 구형 Android용 fallback
                getProcessMapFallback(deviceId)
            }
        } catch (e: Exception) {
            // 방법 2: 구형 Android용 fallback
            getProcessMapFallback(deviceId)
        }
    }
    
    /**
     * 구형 Android 디바이스용 fallback.
     * `adb shell ps` 명령의 다른 형식을 파싱합니다.
     */
    private suspend fun getProcessMapFallback(deviceId: String): Map<Int, String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "-s", deviceId, "shell", "ps")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            parseProcessListFallback(output)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * PID-패키지명 매핑을 주기적으로 갱신하는 Flow를 반환합니다.
     */
    fun observeProcessMap(deviceId: String, intervalMs: Long = 2000): Flow<Map<Int, String>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getProcessMap(deviceId))
            delay(intervalMs)
        }
    }
    
    /**
     * `ps -A -o PID,NAME` 출력을 파싱합니다.
     * 형식: "  PID NAME" 또는 "PID NAME"
     */
    private fun parseProcessList(output: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        
        output.lines()
            .drop(1) // 헤더 스킵
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.trim().split("\\s+".toRegex(), limit = 2)
                if (parts.size >= 2) {
                    val pid = parts[0].toIntOrNull()
                    val name = parts[1].trim()
                    if (pid != null && name.isNotBlank()) {
                        result[pid] = name
                    }
                }
            }
        
        return result
    }
    
    /**
     * 구형 `ps` 출력을 파싱합니다.
     * 형식: "USER PID PPID VSIZE RSS WCHAN PC NAME"
     */
    private fun parseProcessListFallback(output: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        
        output.lines()
            .drop(1) // 헤더 스킵
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.trim().split("\\s+".toRegex())
                // 최소 9개 컬럼 필요: USER PID PPID VSIZE RSS WCHAN PC S NAME
                if (parts.size >= 9) {
                    val pid = parts[1].toIntOrNull()
                    val name = parts.last() // 마지막 컬럼이 NAME
                    if (pid != null && name.isNotBlank()) {
                        result[pid] = name
                    }
                }
            }
        
        return result
    }
    
    private fun parseDeviceList(output: String): List<Device> {
        return output.lines()
            .drop(1) // "List of devices attached" 라인 스킵
            .filter { it.isNotBlank() && !it.startsWith("*") }
            .mapNotNull { line ->
                parseDeviceLine(line)
            }
    }
    
    private fun parseDeviceLine(line: String): Device? {
        // 형식: "emulator-5554 device product:sdk_phone model:sdk_phone device:generic"
        // 또는: "XXXXXXXX device usb:1-1 product:xxx model:xxx device:xxx"
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 2) return null
        
        val id = parts[0]
        val state = DeviceState.fromString(parts[1])
        
        // model 또는 device 정보 추출
        val modelPart = parts.find { it.startsWith("model:") }
        val name = modelPart?.removePrefix("model:")?.replace("_", " ") ?: ""
        
        return Device(
            id = id,
            name = name,
            state = state,
            type = DeviceType.fromDeviceId(id)
        )
    }
}
