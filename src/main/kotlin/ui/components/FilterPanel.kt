package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ui.theme.AppColors

/**
 * 필터 키워드 정의
 */
data class FilterKeyword(
    val keyword: String,
    val description: String
)

private val filterKeywords = listOf(
    // Package
    FilterKeyword("package:", "Package name contains string"),
    FilterKeyword("package=:", "Package name is exactly string"),
    FilterKeyword("package~:", "Package name matches regex"),
    FilterKeyword("-package:", "Package name does not contain string"),
    FilterKeyword("-package=:", "Package name is not exactly string"),
    FilterKeyword("-package~:", "Package name does not match regex"),
    // Process (alias for package)
    FilterKeyword("process:", "Process name contains string"),
    FilterKeyword("process=:", "Process name is exactly string"),
    FilterKeyword("process~:", "Process name matches regex"),
    FilterKeyword("-process:", "Process name does not contain string"),
    FilterKeyword("-process=:", "Process name is not exactly string"),
    FilterKeyword("-process~:", "Process name does not match regex"),
    // Tag
    FilterKeyword("tag:", "Tag contains string"),
    FilterKeyword("tag=:", "Tag is exactly string"),
    FilterKeyword("tag~:", "Tag matches regex"),
    FilterKeyword("-tag:", "Tag does not contain string"),
    FilterKeyword("-tag=:", "Tag is not exactly string"),
    FilterKeyword("-tag~:", "Tag does not match regex"),
    // Message
    FilterKeyword("message:", "Message contains string"),
    FilterKeyword("message=:", "Message is exactly string"),
    FilterKeyword("message~:", "Message matches regex"),
    FilterKeyword("-message:", "Message does not contain string"),
    FilterKeyword("-message=:", "Message is not exactly string"),
    FilterKeyword("-message~:", "Message does not match regex"),
    // Level
    FilterKeyword("level:", "Log level is at least (v/d/i/w/e)"),
    FilterKeyword("level=:", "Log level is exactly"),
    FilterKeyword("-level:", "Log level is not at least"),
    FilterKeyword("-level=:", "Log level is not exactly"),
    // PID/TID
    FilterKeyword("pid:", "Process ID is"),
    FilterKeyword("-pid:", "Process ID is not"),
    FilterKeyword("tid:", "Thread ID is"),
    FilterKeyword("-tid:", "Thread ID is not"),
    // Shortcuts
    FilterKeyword("pkg:", "Package name contains (shortcut)"),
    FilterKeyword("msg:", "Message contains (shortcut)"),
    FilterKeyword("lvl:", "Log level (shortcut)")
)

@Composable
fun FilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.SurfaceVariant,
        tonalElevation = 1.dp
    ) {
        // 통합 검색 필터 with 자동완성
        QueryTextFieldWithAutocomplete(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}

@Composable
private fun QueryTextFieldWithAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // TextFieldValue를 로컬 상태로 관리하여 커서 위치 유지
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    
    // 외부에서 value가 변경되었을 때 동기화 (프로그래매틱 변경 대응)
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length) // 커서를 끝으로 이동
            )
        }
    }
    
    var showSuggestions by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    
    // 현재 입력 중인 단어와 매칭되는 키워드 찾기
    val currentWord = remember(textFieldValue.text) {
        val text = textFieldValue.text
        val lastSpaceIndex = text.lastIndexOf(' ')
        if (lastSpaceIndex == -1) text else text.substring(lastSpaceIndex + 1)
    }
    
    val suggestions = remember(currentWord) {
        if (currentWord.isEmpty()) {
            emptyList()
        } else {
            filterKeywords.filter { 
                it.keyword.startsWith(currentWord, ignoreCase = true) &&
                it.keyword != currentWord
            }
        }
    }
    
    // 포커스가 있고 suggestions가 있을 때만 표시
    val shouldShowDropdown = isFocused && suggestions.isNotEmpty() && currentWord.isNotEmpty()
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue.text)
                showSuggestions = true
            },
            placeholder = {
                Text(
                    "Filter logcat... (type 'p' for suggestions)",
                    color = AppColors.OnSurface.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.OnSurface.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (textFieldValue.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            textFieldValue = TextFieldValue("")
                            onValueChange("")
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                focusedContainerColor = AppColors.Background,
                unfocusedContainerColor = AppColors.Background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }
        )
        
        // 자동완성 드롭다운
        if (shouldShowDropdown) {
            Popup(
                alignment = Alignment.TopStart,
                properties = PopupProperties(focusable = false)
            ) {
                Surface(
                    modifier = Modifier
                        .width(500.dp)
                        .heightIn(max = 300.dp)
                        .padding(top = 52.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Surface,
                    tonalElevation = 8.dp
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(suggestions) { suggestion ->
                            SuggestionItem(
                                keyword = suggestion.keyword,
                                description = suggestion.description,
                                matchedPrefix = currentWord,
                                onClick = {
                                    // 현재 단어를 선택한 키워드로 교체
                                    val text = textFieldValue.text
                                    val lastSpaceIndex = text.lastIndexOf(' ')
                                    val newText = if (lastSpaceIndex == -1) {
                                        suggestion.keyword
                                    } else {
                                        text.substring(0, lastSpaceIndex + 1) + suggestion.keyword
                                    }
                                    textFieldValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newText.length)
                                    )
                                    onValueChange(newText)
                                    showSuggestions = false
                                }
                            )
                        }
                        
                        // 힌트 추가
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.SurfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Combine with OR: \"tag:foo tag:bar\" means \"foo or bar\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    keyword: String,
    description: String,
    matchedPrefix: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 키워드 (매칭된 부분 하이라이트)
        Text(
            text = buildAnnotatedString {
                // 매칭된 부분 (파란색)
                withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold)) {
                    append(keyword.take(matchedPrefix.length))
                }
                // 나머지 부분
                withStyle(SpanStyle(color = AppColors.OnSurface)) {
                    append(keyword.drop(matchedPrefix.length))
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 설명
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.OnSurface.copy(alpha = 0.5f)
        )
    }
}

