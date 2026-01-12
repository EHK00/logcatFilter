package ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

// 다크 테마 색상
object AppColors {
    val Background = Color(0xFF1E1E1E)
    val Surface = Color(0xFF252526)
    val SurfaceVariant = Color(0xFF2D2D30)
    val OnBackground = Color(0xFFD4D4D4)
    val OnSurface = Color(0xFFCCCCCC)
    val Primary = Color(0xFF569CD6)
    val PrimaryVariant = Color(0xFF4FC3F7)
    val Secondary = Color(0xFF9CDCFE)
    val Error = Color(0xFFF44336)
    
    // 로그 레벨 색상
    val LogVerbose = Color(0xFFBBBBBB)
    val LogDebug = Color(0xFF2196F3)
    val LogInfo = Color(0xFF4CAF50)
    val LogWarn = Color(0xFFFF9800)
    val LogError = Color(0xFFF44336)
    val LogFatal = Color(0xFFFF0000)
    
    // UI 요소 색상
    val Border = Color(0xFF3C3C3C)
    val Divider = Color(0xFF404040)
    val SelectedRow = Color(0xFF094771)
    val HoverRow = Color(0xFF2A2D2E)
    
    // 하이라이트 색상
    val Highlight = Color(0xFFFFD54F)  // 밝은 노란색
    val HighlightText = Color(0xFF1E1E1E)  // 하이라이트 텍스트 색상 (어두운 색)
}

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryVariant,
    secondary = AppColors.Secondary,
    onSecondary = Color.Black,
    background = AppColors.Background,
    onBackground = AppColors.OnBackground,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant,
    error = AppColors.Error,
    onError = Color.White
)

// 모노스페이스 폰트
val MonoFontFamily = FontFamily.Monospace

// 타이포그래피
val AppTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = Typography().labelMedium.copy(
        fontSize = 12.sp
    ),
    labelSmall = Typography().labelSmall.copy(
        fontSize = 11.sp
    )
)

@Composable
fun LogcatFilterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
