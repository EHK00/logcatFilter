package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import domain.model.ColumnConfig
import domain.model.LogColumn
import domain.model.LogEntry
import domain.model.LogLevel
import ui.theme.AppColors
import ui.theme.MonoFontFamily
import java.awt.Cursor

@Composable
fun LogTable(
    logs: List<LogEntry>,
    columnConfig: ColumnConfig,
    autoScrollEnabled: Boolean,
    highlightTerms: List<String> = emptyList(),
    scrollToIndex: Int? = null,
    onScrollComplete: () -> Unit = {},
    onColumnResize: (LogColumn, Dp) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val logsSize = logs.size
    
    // 자동 스크롤: 새 로그가 추가될 때마다 마지막으로 스크롤
    LaunchedEffect(logsSize) {
        if (autoScrollEnabled && logsSize > 0) {
            listState.scrollToItem(logsSize - 1)
        }
    }
    
    // FAB 클릭 시 즉시 마지막으로 스크롤
    LaunchedEffect(autoScrollEnabled) {
        if (autoScrollEnabled && logsSize > 0) {
            listState.scrollToItem(logsSize - 1)
        }
    }
    
    // 검색 결과로 스크롤
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex != null && scrollToIndex in logs.indices) {
            listState.animateScrollToItem(scrollToIndex)
            onScrollComplete()
        }
    }
    
    // 사용자가 위로 스크롤하면 자동 스크롤 비활성화
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem == null || lastVisibleItem.index >= logsSize - 1
        }
    }
    
    // 스크롤 중이고 맨 아래가 아니면 자동 스크롤 끄기
    LaunchedEffect(listState.isScrollInProgress, isAtBottom) {
        if (listState.isScrollInProgress && !isAtBottom && autoScrollEnabled) {
            onAutoScrollChange(false)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 헤더
        LogTableHeader(
            columnConfig = columnConfig,
            onColumnResize = onColumnResize
        )
        
        Divider(color = AppColors.Divider)
        
        // 로그 목록
        Box(modifier = Modifier.weight(1f)) {
            if (logs.isEmpty()) {
                EmptyLogPlaceholder()
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = logs,
                            key = { it.id }
                        ) { entry ->
                            LogRow(
                                entry = entry,
                                columnConfig = columnConfig,
                                highlightTerms = highlightTerms
                            )
                            Divider(
                                color = AppColors.Divider.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
            
            // 자동 스크롤 토글 FAB (항상 표시)
            if (logs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onAutoScrollChange(!autoScrollEnabled) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(44.dp),
                    containerColor = if (autoScrollEnabled) AppColors.Primary else AppColors.Surface,
                    contentColor = if (autoScrollEnabled) Color.White else AppColors.OnSurface.copy(alpha = 0.6f)
                ) {
                    Icon(
                        imageVector = if (autoScrollEnabled) 
                            Icons.Default.VerticalAlignBottom 
                        else 
                            Icons.Default.VerticalAlignBottom,
                        contentDescription = if (autoScrollEnabled) "Auto-scroll ON" else "Auto-scroll OFF",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogTableHeader(
    columnConfig: ColumnConfig,
    onColumnResize: (LogColumn, Dp) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(AppColors.SurfaceVariant)
            .padding(vertical = 8.dp)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순서대로 컬럼 헤더 렌더링
        columnConfig.visibleColumns.forEach { column ->
            when (column) {
                LogColumn.MESSAGE -> {
                    // Message는 가변 너비
                    Text(
                        text = column.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.OnSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }
                else -> {
                    ResizableHeaderCell(
                        text = column.displayName,
                        width = columnConfig.getWidth(column),
                        column = column,
                        onResize = onColumnResize
                    )
                }
            }
        }
    }
}

@Composable
private fun ResizableHeaderCell(
    text: String,
    width: Dp,
    column: LogColumn,
    onResize: (LogColumn, Dp) -> Unit
) {
    val density = LocalDensity.current
    var currentWidth by remember(width) { mutableStateOf(width) }
    
    Row(
        modifier = Modifier.width(currentWidth),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.OnSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // 리사이즈 핸들
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(20.dp)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(column) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newWidth = with(density) {
                                (currentWidth.toPx() + dragAmount.x).toDp()
                            }
                            val minWidth = column.minWidth
                            if (newWidth >= minWidth) {
                                currentWidth = newWidth
                            }
                        },
                        onDragEnd = {
                            onResize(column, currentWidth)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp),
                color = AppColors.Border
            )
        }
    }
}

@Composable
fun LogRow(
    entry: LogEntry,
    columnConfig: ColumnConfig,
    highlightTerms: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .padding(vertical = 4.dp)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 순서대로 컬럼 셀 렌더링
        columnConfig.visibleColumns.forEach { column ->
            when (column) {
                LogColumn.TIME -> {
                    Text(
                        text = entry.formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        color = AppColors.OnSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.width(columnConfig.getWidth(column))
                    )
                }
                LogColumn.PACKAGE -> {
                    ProcessCell(
                        packageName = entry.packageName,
                        pid = entry.pid,
                        modifier = Modifier.width(columnConfig.getWidth(column))
                    )
                }
                LogColumn.TID -> {
                    Text(
                        text = entry.tid.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        color = AppColors.OnSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.width(columnConfig.getWidth(column))
                    )
                }
                LogColumn.LEVEL -> {
                    LogLevelBadge(
                        level = entry.level,
                        modifier = Modifier.width(columnConfig.getWidth(column))
                    )
                }
                LogColumn.TAG -> {
                    Text(
                        text = entry.tag,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        color = AppColors.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(columnConfig.getWidth(column))
                    )
                }
                LogColumn.MESSAGE -> {
                    // Message (하이라이트 적용)
                    val highlightedMessage = remember(entry.message, highlightTerms) {
                        buildHighlightedText(entry.message, highlightTerms, entry.level.color)
                    }
                    
                    Text(
                        text = highlightedMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 검색어를 하이라이트하여 AnnotatedString 생성
 */
private fun buildHighlightedText(
    text: String,
    highlightTerms: List<String>,
    defaultColor: Color
): AnnotatedString {
    if (highlightTerms.isEmpty()) {
        return AnnotatedString(text, SpanStyle(color = defaultColor))
    }
    
    return buildAnnotatedString {
        // 기본 스타일 적용
        withStyle(SpanStyle(color = defaultColor)) {
            var currentIndex = 0
            
            // 모든 하이라이트 위치 찾기
            val highlights = mutableListOf<Pair<Int, Int>>()  // (start, end)
            
            for (term in highlightTerms) {
                if (term.isBlank()) continue
                
                var searchStart = 0
                while (true) {
                    val index = text.indexOf(term, searchStart, ignoreCase = true)
                    if (index == -1) break
                    highlights.add(index to (index + term.length))
                    searchStart = index + 1
                }
            }
            
            // 위치순 정렬 및 겹침 제거
            val sortedHighlights = highlights.sortedBy { it.first }
            val mergedHighlights = mutableListOf<Pair<Int, Int>>()
            
            for (highlight in sortedHighlights) {
                if (mergedHighlights.isEmpty() || mergedHighlights.last().second < highlight.first) {
                    mergedHighlights.add(highlight)
                } else {
                    // 겹치는 경우 병합
                    val last = mergedHighlights.removeLast()
                    mergedHighlights.add(last.first to maxOf(last.second, highlight.second))
                }
            }
            
            // AnnotatedString 구성
            for ((start, end) in mergedHighlights) {
                // 하이라이트 이전 텍스트
                if (currentIndex < start) {
                    append(text.substring(currentIndex, start))
                }
                
                // 하이라이트 적용
                pushStyle(SpanStyle(
                    color = AppColors.Background,  // 어두운 배경에서 잘 보이도록
                    background = AppColors.Highlight
                ))
                append(text.substring(start, end))
                pop()
                
                currentIndex = end
            }
            
            // 나머지 텍스트
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
}

/**
 * 패키지명 또는 PID를 표시하는 셀
 * 컬럼 너비에 따라 자동으로 말줄임 적용
 */
@Composable
private fun ProcessCell(
    packageName: String?,
    pid: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        if (packageName != null) {
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = MonoFontFamily,
                color = AppColors.PrimaryVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = pid.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = MonoFontFamily,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LogLevelBadge(
    level: LogLevel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = level.char.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = MonoFontFamily,
            color = level.color
        )
    }
}

@Composable
private fun BoxScope.EmptyLogPlaceholder() {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No logs to display",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.OnSurface.copy(alpha = 0.5f)
        )
        Text(
            text = "Select a device and click Start to begin capturing logs",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.OnSurface.copy(alpha = 0.3f)
        )
    }
}
