package app.jpcard.reader.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

/** Everything the screens can ask for. Screens read state from [MainViewModel] and call these. */
class AppActions(
    val openCard: (idm: String) -> Unit,
    val back: () -> Unit,
    val openSettings: () -> Unit,
    val setLanguage: (tag: String) -> Unit,
    val rename: (idm: String, name: String) -> Unit,
    val delete: (idm: String) -> Unit,
    val pickFace: (idm: String, uri: Uri) -> Unit,
    val confirmCrop: (bitmap: Bitmap, area: Rect) -> Unit,
    val cancelCrop: () -> Unit,
    val clearFace: (idm: String) -> Unit,
    val dismissError: () -> Unit,
    val openNfcSettings: () -> Unit,
)

private const val SETTINGS = "#settings"

/** Root composable: picks home / detail / settings and overlays the crop screen when needed. */
@Composable
fun App(state: MainViewModel, actions: AppActions) {
    val selected = state.selected
    BackHandler(enabled = selected != null || state.showSettings) { actions.back() }

    AnimatedContent(
        targetState = if (state.showSettings) SETTINGS else selected?.idm,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { key ->
        if (key == SETTINGS) {
            SettingsScreen(actions)
        } else {
            val card = state.cards.find { it.idm == key }
            if (card == null) {
                HomeScreen(state, actions)
            } else {
                DetailScreen(card, state, actions)
            }
        }
    }

    state.crop?.let { req ->
        CropDialog(req.bitmap, onCancel = actions.cancelCrop, onConfirm = actions.confirmCrop)
    }
}
