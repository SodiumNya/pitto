package app.jpcard.reader.ui

import androidx.compose.runtime.getValue
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

    val selected: SavedCard? get() = cards.find { it.idm == selectedIdm }
}

class AppActions(
    val openCard: (String) -> Unit,
    val back: () -> Unit,
    val rename: (String, String) -> Unit,
    val delete: (String) -> Unit,
    val dismissError: () -> Unit,
    val openNfcSettings: () -> Unit,
)
