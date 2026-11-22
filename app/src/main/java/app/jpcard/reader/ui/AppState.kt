package app.jpcard.reader.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.jpcard.reader.data.SavedCard

enum class NfcStatus { READY, DISABLED, UNSUPPORTED }

class AppState {
    var cards by mutableStateOf<List<SavedCard>>(emptyList())
    var selectedIdm by mutableStateOf<String?>(null)
    var reading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var nfcStatus by mutableStateOf(NfcStatus.READY)
    /** 站名表加载完成后置 true，触发界面刷新 */
    var stationsReady by mutableStateOf(false)

    /** 每次读卡失败 +1，用来触发提示图标的抖动 */
    var errorTick by mutableIntStateOf(0)

    /** 每次读卡成功 +1；详情页据此播放一次“已更新”动画 */
    var readTick by mutableIntStateOf(0)
        private set
    var readIdm: String? = null
        private set
    var consumedTick = 0

    fun celebrate(idm: String) {
        readIdm = idm
        readTick++
    }

    val selected: SavedCard? get() = cards.find { it.idm == selectedIdm }
}

class AppActions(
    val openCard: (String) -> Unit,
    val back: () -> Unit,
    val rename: (String, String) -> Unit,
    val delete: (String) -> Unit,
    val restore: (SavedCard) -> Unit,
    val dismissError: () -> Unit,
    val openNfcSettings: () -> Unit,
)
