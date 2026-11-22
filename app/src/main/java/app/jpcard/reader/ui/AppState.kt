package app.jpcard.reader.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.jpcard.reader.data.SavedCard

class CropRequest(val idm: String, val bitmap: Bitmap)

enum class NfcStatus { READY, DISABLED, UNSUPPORTED }

class AppState {
    var cards by mutableStateOf<List<SavedCard>>(emptyList())
    var selectedIdm by mutableStateOf<String?>(null)
    var reading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var nfcStatus by mutableStateOf(NfcStatus.READY)
    /** 站名表加载完成后置 true，触发界面刷新 */
    var stationsReady by mutableStateOf(false)

    /** 自定义卡面变化时 +1，让界面重新加载图片 */
    var faceVersion by mutableIntStateOf(0)
    /** 正在处理用户选的卡面图片 */
    var faceBusy by mutableStateOf(false)
    /** 非 null 时显示裁剪界面 */
    var crop by mutableStateOf<CropRequest?>(null)

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
    val setFace: (String, Uri) -> Unit,
    val confirmCrop: (Bitmap, Rect) -> Unit,
    val cancelCrop: () -> Unit,
    val clearFace: (String) -> Unit,
    val dismissError: () -> Unit,
    val openNfcSettings: () -> Unit,
)
