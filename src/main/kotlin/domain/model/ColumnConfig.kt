package domain.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 로그 테이블의 컬럼 타입
 */
enum class LogColumn(
    val displayName: String,
    val tag: String,  // 텍스트 설정용 태그
    val defaultWidth: Dp,
    val minWidth: Dp
) {
    TIME("Time", "time", 145.dp, 100.dp),
    PACKAGE("Package/PID", "package", 180.dp, 80.dp),
    TID("TID", "tid", 50.dp, 40.dp),
    LEVEL("Lvl", "lvl", 35.dp, 30.dp),
    TAG("Tag", "tag", 140.dp, 60.dp),
    MESSAGE("Message", "message", 0.dp, 100.dp);  // Message는 가변 너비
    
    companion object {
        /** 기본 컬럼 순서 */
        val DEFAULT_ORDER = listOf(TIME, PACKAGE, TID, LEVEL, TAG, MESSAGE)
        
        /** 기본 컬럼 설정 텍스트 */
        const val DEFAULT_CONFIG_TEXT = "{time}{package}{tid}{lvl}{tag}{message}"
        
        /** 태그로 컬럼 찾기 */
        fun fromTag(tag: String): LogColumn? = entries.find { it.tag == tag }
    }
}

/**
 * 개별 컬럼 설정
 */
data class ColumnState(
    val column: LogColumn,
    val width: Dp = column.defaultWidth,
    val visible: Boolean = true
)

/**
 * 전체 컬럼 설정
 */
data class ColumnConfig(
    val columns: Map<LogColumn, ColumnState> = LogColumn.entries.associateWith { 
        ColumnState(column = it) 
    },
    val columnOrder: List<LogColumn> = LogColumn.DEFAULT_ORDER
) {
    fun getWidth(column: LogColumn): Dp = columns[column]?.width ?: column.defaultWidth
    
    fun isVisible(column: LogColumn): Boolean = columns[column]?.visible ?: true
    
    fun setWidth(column: LogColumn, width: Dp): ColumnConfig {
        val minWidth = column.minWidth
        val adjustedWidth = maxOf(width, minWidth)
        val currentState = columns[column] ?: ColumnState(column)
        return copy(
            columns = columns + (column to currentState.copy(width = adjustedWidth))
        )
    }
    
    fun toggleVisibility(column: LogColumn): ColumnConfig {
        val currentState = columns[column] ?: ColumnState(column)
        return copy(
            columns = columns + (column to currentState.copy(visible = !currentState.visible))
        )
    }
    
    fun setVisibility(column: LogColumn, visible: Boolean): ColumnConfig {
        val currentState = columns[column] ?: ColumnState(column)
        return copy(
            columns = columns + (column to currentState.copy(visible = visible))
        )
    }
    
    /**
     * 표시되는 컬럼 목록 (순서 적용)
     */
    val visibleColumns: List<LogColumn>
        get() = columnOrder.filter { isVisible(it) }
    
    /**
     * 컬럼 설정을 텍스트로 변환
     * 예: {time}{package}{tid}{lvl}{tag}{message}
     */
    fun toConfigText(): String {
        return visibleColumns.joinToString("") { "{${it.tag}}" }
    }
    
    companion object {
        /**
         * 텍스트에서 컬럼 설정을 파싱
         * 예: {time}{package}{tid}{lvl}{tag}{message}
         */
        fun fromConfigText(text: String): ColumnConfig {
            val tagPattern = "\\{([^}]+)\\}".toRegex()
            val parsedColumns = tagPattern.findAll(text)
                .mapNotNull { match -> 
                    LogColumn.fromTag(match.groupValues[1].lowercase())
                }
                .distinct()
                .toList()
            
            // 파싱된 컬럼이 없으면 기본값 반환
            if (parsedColumns.isEmpty()) {
                return ColumnConfig()
            }
            
            // 보이는 컬럼 설정
            val visibleSet = parsedColumns.toSet()
            val columnStates = LogColumn.entries.associateWith { column ->
                ColumnState(column = column, visible = column in visibleSet)
            }
            
            // 순서 설정: 파싱된 순서 + 나머지 컬럼
            val remainingColumns = LogColumn.entries.filter { it !in visibleSet }
            val newOrder = parsedColumns + remainingColumns
            
            return ColumnConfig(columns = columnStates, columnOrder = newOrder)
        }
        
        /**
         * 기본 설정으로 초기화
         */
        fun default(): ColumnConfig = ColumnConfig()
    }
}
