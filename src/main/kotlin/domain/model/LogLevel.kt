package domain.model

import androidx.compose.ui.graphics.Color

enum class LogLevel(
    val char: Char,
    val priority: Int,
    val displayName: String,
    val color: Color
) {
    VERBOSE('V', 0, "Verbose", Color(0xFFBBBBBB)),
    DEBUG('D', 1, "Debug", Color(0xFF2196F3)),
    INFO('I', 2, "Info", Color(0xFF4CAF50)),
    WARN('W', 3, "Warn", Color(0xFFFF9800)),
    ERROR('E', 4, "Error", Color(0xFFF44336)),
    FATAL('F', 5, "Fatal", Color(0xFFFF0000));

    companion object {
        fun fromChar(char: Char): LogLevel? {
            return entries.find { it.char == char.uppercaseChar() }
        }
    }
}
