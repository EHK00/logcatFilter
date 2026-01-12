package domain.model

/**
 * 애플리케이션 설정을 담는 데이터 클래스입니다.
 */
data class AppSettings(
    /** 로그 자동 저장 디렉토리 */
    val logSaveDirectory: String = DEFAULT_LOG_SAVE_DIR,
    
    /** Stop 시 자동 저장 여부 */
    val autoSaveOnStop: Boolean = true
) {
    companion object {
        /** 기본 로그 저장 디렉토리 (현재 디렉토리 하위 logs/) */
        const val DEFAULT_LOG_SAVE_DIR = "./logs"
    }
}
