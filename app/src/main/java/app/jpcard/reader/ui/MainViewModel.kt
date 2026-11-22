package app.jpcard.reader.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.jpcard.reader.R
import app.jpcard.reader.data.CardFaces
import app.jpcard.reader.data.CardStore
import app.jpcard.reader.data.SavedCard
import app.jpcard.reader.data.Stations
import app.jpcard.reader.nfc.CardData
import app.jpcard.reader.nfc.CardReadException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A photo the user picked, waiting in the crop screen. */
class CropRequest(val idm: String, val bitmap: Bitmap)

enum class NfcStatus { READY, DISABLED, UNSUPPORTED }

/**
 * UI state and the actions that change it.
 *
 * Living in a ViewModel means the state survives activity re-creation
 * (e.g. when the app language changes), so the user stays on the same screen.
 * Anything that needs an Activity (NFC reader mode, launching settings) stays in MainActivity.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CardStore(app)

    var cards by mutableStateOf(store.all())
        private set
    var selectedIdm by mutableStateOf<String?>(null)
        private set
    var showSettings by mutableStateOf(false)
        private set
    var reading by mutableStateOf(false)
        private set
    var nfcStatus by mutableStateOf(NfcStatus.READY)

    /** String resource of the error banner, or null when there is none. */
    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    /** Becomes true once the station table is loaded, so history rows re-render with names. */
    var stationsReady by mutableStateOf(Stations.loaded)
        private set

    /** Bumped whenever a custom card face changes, so images are reloaded. */
    var faceVersion by mutableIntStateOf(0)
        private set

    /** True while a picked photo is being decoded or saved. */
    var faceBusy by mutableStateOf(false)
        private set

    /** Non-null while the crop screen is shown. */
    var crop by mutableStateOf<CropRequest?>(null)
        private set

    /** Bumped on every failed read; drives the shake of the tap hint. */
    var errorTick by mutableIntStateOf(0)
        private set

    /** Bumped on every successful read; the detail screen plays its "updated" animation once per tick. */
    var readTick by mutableIntStateOf(0)
        private set
    var readIdm: String? = null
        private set

    /** Last [readTick] the detail screen has already animated for. */
    var consumedTick = 0

    val selected: SavedCard? get() = cards.find { it.idm == selectedIdm }

    init {
        if (!Stations.loaded) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { runCatching { Stations.load(app) } }
                stationsReady = Stations.loaded
            }
        }
    }

    // ---- Navigation ----

    fun openCard(idm: String) {
        selectedIdm = idm
    }

    fun openSettings() {
        showSettings = true
    }

    fun back() {
        if (showSettings) showSettings = false else selectedIdm = null
    }

    // ---- Card reading (called from the NFC callback via MainActivity) ----

    fun onReadStarted() {
        reading = true
        error = null
    }

    fun onReadFinished(result: Result<CardData>) {
        reading = false
        result.onSuccess { data ->
            val card = store.save(data)
            cards = store.all()
            selectedIdm = card.idm
            readIdm = card.idm
            readTick++
        }.onFailure {
            error = (it as? CardReadException)?.reason ?: R.string.err_read_failed
            errorTick++
        }
    }

    fun setNfcState(status: NfcStatus) {
        nfcStatus = status
        if (status != NfcStatus.READY) reading = false
    }

    fun dismissError() {
        error = null
    }

    // ---- Card management ----

    fun rename(idm: String, name: String) {
        store.rename(idm, name)
        cards = store.all()
    }

    fun delete(idm: String) {
        store.delete(idm)
        CardFaces.delete(getApplication(), idm)
        cards = store.all()
        if (selectedIdm == idm) selectedIdm = null
    }

    // ---- Custom card faces ----

    /** Decodes the picked photo at an editable size, then opens the crop screen. */
    fun pickFace(idm: String, uri: Uri) {
        faceBusy = true
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { CardFaces.decodeForEdit(getApplication(), uri) }
            faceBusy = false
            if (bitmap == null) error = R.string.err_image else crop = CropRequest(idm, bitmap)
        }
    }

    fun confirmCrop(bitmap: Bitmap, area: Rect) {
        val idm = crop?.idm ?: return
        crop = null
        faceBusy = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { CardFaces.save(getApplication(), idm, bitmap, area) }
            faceBusy = false
            faceVersion++
            if (!ok) error = R.string.err_face_save
        }
    }

    fun cancelCrop() {
        crop = null
    }

    fun clearFace(idm: String) {
        CardFaces.delete(getApplication(), idm)
        faceVersion++
    }
}
