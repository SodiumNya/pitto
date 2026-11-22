package app.jpcard.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.jpcard.reader.nfc.CardType
import app.jpcard.reader.nfc.HistoryEntry

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

/** 各卡种的卡面主色（取各卡的代表色，近似值；不使用任何官方标志）。白色文字都能看清。 */
fun cardColor(type: CardType): Color = when (type) {
    CardType.SUICA -> Color(0xFF00924A)
    CardType.ICOCA -> Color(0xFF0071BC)
    CardType.PASMO -> Color(0xFFD6006F)
    CardType.TOICA -> Color(0xFFD96A00)
    CardType.MANACA -> Color(0xFF7B4BA0)
    CardType.KITACA -> Color(0xFF4F8A2B)
    CardType.NIMOCA -> Color(0xFFC08000)
    CardType.PITAPA -> Color(0xFF1F3A68)
    CardType.SUGOCA -> Color(0xFF1A8CC4)
    CardType.HAYAKAKEN -> Color(0xFFD7263D)
    CardType.UNKNOWN -> Color(0xFF0E7C66)
}

/** 记录类别的标记色。浅色、深色模式下都清晰；OTHER 用中性灰兜底。 */
@Composable
fun kindColor(kind: HistoryEntry.Kind): Color {
    val dark = isSystemInDarkTheme()
    return when (kind) {
        HistoryEntry.Kind.TRAIN -> if (dark) Color(0xFF8AB4F8) else Color(0xFF2F6FDE)
        HistoryEntry.Kind.BUS -> if (dark) Color(0xFFFFB067) else Color(0xFFD56F00)
        HistoryEntry.Kind.SHOPPING -> if (dark) Color(0xFFD7A6F2) else Color(0xFF9A46C2)
        HistoryEntry.Kind.CHARGE -> if (dark) Color(0xFF7FD1B9) else Color(0xFF0E7C66)
        HistoryEntry.Kind.OTHER -> if (dark) Color(0xFFA0A6B0) else Color(0xFF7A808A)
    }
}

/** 收入（充值）用绿色，支出保持正文色，避免满屏红色。 */
val IncomeColor @Composable get() = if (isSystemInDarkTheme()) Color(0xFF7FD1B9) else Color(0xFF0E7C66)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // 固定品牌色，不跟随壁纸取色，保证界面风格一致
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
