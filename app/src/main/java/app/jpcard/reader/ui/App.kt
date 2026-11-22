package app.jpcard.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

@Composable
fun App(state: AppState, actions: AppActions) {
    val selected = state.selected
    BackHandler(enabled = selected != null) { actions.back() }

    AnimatedContent(
        targetState = selected?.idm,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { idm ->
        val card = state.cards.find { it.idm == idm }
        if (card == null) {
            HomeScreen(state, actions)
        } else {
            DetailScreen(card, state, actions)
        }
    }
}
