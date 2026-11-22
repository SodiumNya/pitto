package app.jpcard.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

private const val SETTINGS = "#settings"

@Composable
fun App(state: AppState, actions: AppActions) {
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
