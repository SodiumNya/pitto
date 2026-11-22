package app.jpcard.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0E7C66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFE4),
    onPrimaryContainer = Color(0xFF00382C),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD1B9),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFCDEFE4),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
)

/** 收入（充值）用绿色，支出保持正文色，避免满屏红色。 */
val IncomeColor @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7FD1B9) else Color(0xFF0E7C66)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // 固定品牌色，不跟随壁纸取色，保证界面风格一致
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
