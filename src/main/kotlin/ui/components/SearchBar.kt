package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ui.theme.AppColors
import ui.theme.MonoFontFamily

/**
 * Android Studio 스타일의 검색 바
 * 필터와 별도로 동작하며, 검색 결과 탐색 기능 제공
 */
@Composable
fun SearchBar(
    query: String,
    matchCount: Int,
    currentIndex: Int,
    onQueryChange: (String) -> Unit,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    // TextFieldValue를 로컬 상태로 관리하여 커서 위치 유지
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query)) }
    
    // 외부에서 query가 변경되었을 때 동기화 (프로그래매틱 변경 대응)
    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length) // 커서를 끝으로 이동
            )
        }
    }
    
    // 검색 바가 열리면 포커스
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.SurfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 검색 아이콘
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AppColors.OnSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            
            // 검색 입력창
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onQueryChange(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                event.key == Key.Enter && event.isShiftPressed -> {
                                    onPrevMatch()
                                    true
                                }
                                event.key == Key.Enter -> {
                                    onNextMatch()
                                    true
                                }
                                event.key == Key.Escape -> {
                                    onClose()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                textStyle = TextStyle(
                    fontFamily = MonoFontFamily,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = AppColors.OnSurface
                ),
                cursorBrush = SolidColor(AppColors.Primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = AppColors.Surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Search in logs...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.OnSurface.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            // 검색 결과 카운트
            if (query.isNotEmpty()) {
                Text(
                    text = if (matchCount > 0) "$currentIndex/$matchCount" else "0/0",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (matchCount > 0) 
                        AppColors.OnSurface.copy(alpha = 0.7f) 
                    else 
                        AppColors.LogError.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            // 이전 결과 버튼
            IconButton(
                onClick = onPrevMatch,
                enabled = matchCount > 0,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Previous match (Shift+Enter)",
                    tint = if (matchCount > 0) 
                        AppColors.OnSurface.copy(alpha = 0.8f) 
                    else 
                        AppColors.OnSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // 다음 결과 버튼
            IconButton(
                onClick = onNextMatch,
                enabled = matchCount > 0,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Next match (Enter)",
                    tint = if (matchCount > 0) 
                        AppColors.OnSurface.copy(alpha = 0.8f) 
                    else 
                        AppColors.OnSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // 닫기 버튼
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close search (Escape)",
                    tint = AppColors.OnSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
