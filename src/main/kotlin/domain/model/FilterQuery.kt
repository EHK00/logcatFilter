package domain.model

/**
 * Android Studio 스타일의 필터 쿼리 파서
 * 
 * 지원 키워드:
 * - package: / package=: / package~:
 * - tag: / tag=: / tag~:
 * - level: (예: level:error, level:warn)
 * - message: / message=: / message~:
 * - pid:
 * 
 * 부정 연산자:
 * - -package: / -tag: 등 앞에 '-'를 붙이면 해당 조건 제외
 * 
 * 매칭 방식:
 * - keyword: (기본) 부분 일치, 대소문자 무시
 * - keyword=: 정확히 일치
 * - keyword~: 정규식 매칭
 * 
 * 조합 규칙:
 * - 동일 키 여러개: OR 연산 (package:foo package:bar → foo OR bar)
 * - 다른 키 조합: AND 연산 (package:foo tag:bar → foo AND bar)
 * - 부정 조건: 모두 AND로 처리
 */
data class FilterQuery(
    val rawQuery: String = "",
    val conditions: List<FilterCondition> = emptyList(),
    val plainText: String = ""  // 키워드가 아닌 일반 텍스트 검색어
) {
    companion object {
        // 지원하는 키워드 패턴: -?keyword(=|~)?:
        private val KEYWORD_PATTERN = Regex(
            """(-)?(\w+)(=|~)?:(\S+|"[^"]*")""",
            RegexOption.IGNORE_CASE
        )
        
        fun parse(query: String): FilterQuery {
            if (query.isBlank()) {
                return FilterQuery()
            }
            
            val conditions = mutableListOf<FilterCondition>()
            var remainingText = query
            
            // 키워드 패턴 찾기
            KEYWORD_PATTERN.findAll(query).forEach { match ->
                val negate = match.groupValues[1] == "-"
                val keyword = match.groupValues[2].lowercase()
                val matchType = when (match.groupValues[3]) {
                    "=" -> MatchType.EXACT
                    "~" -> MatchType.REGEX
                    else -> MatchType.CONTAINS
                }
                var value = match.groupValues[4]
                
                // 따옴표 제거
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length - 1)
                }
                
                val conditionType = when (keyword) {
                    "package", "pkg", "process" -> ConditionType.PACKAGE
                    "tag" -> ConditionType.TAG
                    "level", "lvl" -> ConditionType.LEVEL
                    "message", "msg" -> ConditionType.MESSAGE
                    "pid" -> ConditionType.PID
                    "tid" -> ConditionType.TID
                    else -> null
                }
                
                if (conditionType != null) {
                    conditions.add(
                        FilterCondition(
                            type = conditionType,
                            value = value,
                            matchType = matchType,
                            negate = negate
                        )
                    )
                }
                
                // 처리된 부분 제거
                remainingText = remainingText.replace(match.value, "")
            }
            
            // 남은 텍스트는 일반 검색어로 처리
            val plainText = remainingText.trim()
            
            return FilterQuery(
                rawQuery = query,
                conditions = conditions,
                plainText = plainText
            )
        }
    }
    
    /**
     * 하이라이트 대상 검색어 목록 반환
     * 
     * 하이라이트 대상:
     * - plainText (일반 텍스트 검색어)
     * - message: 조건의 값들 (부정 조건 제외)
     * 
     * 하이라이트 제외:
     * - package:, tag:, level:, pid:, tid: 조건
     * - 부정 조건 (-message:)
     */
    fun getHighlightTerms(): List<String> {
        val terms = mutableListOf<String>()
        
        // message: 조건에서 추출 (부정 조건 제외)
        conditions
            .filter { it.type == ConditionType.MESSAGE && !it.negate }
            .forEach { terms.add(it.value) }
        
        // 일반 텍스트 검색어 추가
        if (plainText.isNotBlank()) {
            terms.add(plainText)
        }
        
        return terms
    }
    
    /**
     * 로그 엔트리가 필터 조건에 맞는지 확인
     * 
     * 동일 타입의 긍정 조건들은 OR로 연결 (하나라도 매칭되면 통과)
     * 부정 조건들은 모두 AND로 처리 (모두 통과해야 함)
     * 다른 타입 간에는 AND로 연결
     */
    fun matches(entry: LogEntry): Boolean {
        // 부정 조건 먼저 처리 (모두 AND - 하나라도 매칭되면 제외)
        val negativeConditions = conditions.filter { it.negate }
        for (condition in negativeConditions) {
            if (!condition.matches(entry)) return false
        }
        
        // 긍정 조건을 타입별로 그룹화
        val positiveConditions = conditions.filter { !it.negate }
        val groupedByType = positiveConditions.groupBy { it.type }
        
        // 각 타입 그룹 내에서 OR 연산, 그룹 간에는 AND 연산
        for ((_, typeConditions) in groupedByType) {
            // 같은 타입의 조건들 중 하나라도 매칭되면 통과 (OR)
            val anyMatch = typeConditions.any { it.matches(entry) }
            if (!anyMatch) return false
        }
        
        // 일반 텍스트 검색 (message, tag, packageName에서 검색)
        if (plainText.isNotBlank()) {
            val matchesPlainText = entry.message.contains(plainText, ignoreCase = true) ||
                    entry.tag.contains(plainText, ignoreCase = true) ||
                    (entry.packageName?.contains(plainText, ignoreCase = true) == true)
            if (!matchesPlainText) return false
        }
        
        return true
    }
}

enum class ConditionType {
    PACKAGE,
    TAG,
    LEVEL,
    MESSAGE,
    PID,
    TID
}

enum class MatchType {
    CONTAINS,  // 부분 일치 (기본)
    EXACT,     // 정확히 일치 (=)
    REGEX      // 정규식 (~)
}

data class FilterCondition(
    val type: ConditionType,
    val value: String,
    val matchType: MatchType = MatchType.CONTAINS,
    val negate: Boolean = false
) {
    private val regexPattern: Regex? by lazy {
        if (matchType == MatchType.REGEX) {
            try {
                Regex(value, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    fun matches(entry: LogEntry): Boolean {
        val result = when (type) {
            ConditionType.PACKAGE -> matchString(entry.packageName ?: entry.pid.toString())
            ConditionType.TAG -> matchString(entry.tag)
            ConditionType.MESSAGE -> matchString(entry.message)
            ConditionType.PID -> matchString(entry.pid.toString())
            ConditionType.TID -> matchString(entry.tid.toString())
            ConditionType.LEVEL -> matchLevel(entry.level)
        }
        
        return if (negate) !result else result
    }
    
    private fun matchString(target: String): Boolean {
        return when (matchType) {
            MatchType.CONTAINS -> target.contains(value, ignoreCase = true)
            MatchType.EXACT -> target.equals(value, ignoreCase = true)
            MatchType.REGEX -> regexPattern?.containsMatchIn(target) == true
        }
    }
    
    private fun matchLevel(level: LogLevel): Boolean {
        val targetLevel = when (value.lowercase()) {
            "v", "verbose" -> LogLevel.VERBOSE
            "d", "debug" -> LogLevel.DEBUG
            "i", "info" -> LogLevel.INFO
            "w", "warn", "warning" -> LogLevel.WARN
            "e", "error" -> LogLevel.ERROR
            "f", "fatal", "a", "assert" -> LogLevel.FATAL
            else -> return false
        }
        
        return when (matchType) {
            MatchType.EXACT -> level == targetLevel
            else -> level.ordinal >= targetLevel.ordinal  // 해당 레벨 이상
        }
    }
}
