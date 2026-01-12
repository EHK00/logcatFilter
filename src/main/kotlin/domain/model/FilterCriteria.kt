package domain.model

data class FilterCriteria(
    val levels: Set<LogLevel> = LogLevel.entries.toSet(),
    val packageName: String = "",
    val pid: String = "",
    val tag: String = "",
    val keyword: String = ""
) {
    fun matches(entry: LogEntry): Boolean {
        // 로그 레벨 체크
        if (entry.level !in levels) return false

        // 패키지명 체크 (부분 일치, 대소문자 무시)
        if (packageName.isNotBlank()) {
            val entryPackage = entry.packageName ?: ""
            if (!entryPackage.contains(packageName, ignoreCase = true)) {
                // 패키지명이 없으면 PID로도 매칭 시도
                if (entry.pid.toString() != packageName) return false
            }
        }

        // PID 체크
        if (pid.isNotBlank()) {
            val pidInt = pid.toIntOrNull()
            if (pidInt != null && entry.pid != pidInt) return false
        }

        // 태그 체크 (부분 일치, 대소문자 무시)
        if (tag.isNotBlank()) {
            if (!entry.tag.contains(tag, ignoreCase = true)) return false
        }

        // 키워드 체크 (메시지에서 검색, 대소문자 무시)
        if (keyword.isNotBlank()) {
            if (!entry.message.contains(keyword, ignoreCase = true) &&
                !entry.tag.contains(keyword, ignoreCase = true)) {
                return false
            }
        }

        return true
    }
}
