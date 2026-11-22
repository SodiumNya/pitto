package app.jpcard.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.jpcard.reader.data.AppLanguage
import app.jpcard.reader.nfc.CardReadException
import app.jpcard.reader.nfc.FelicaReader
import app.jpcard.reader.ui.App
import app.jpcard.reader.ui.AppActions
import app.jpcard.reader.ui.MainViewModel
import app.jpcard.reader.ui.NfcStatus
import app.jpcard.reader.ui.theme.AppTheme

/**
 * The only activity. It owns what needs an Activity: NFC reader mode, the NFC on/off
 * broadcast, sound/vibration feedback and launching system screens.
 * All UI state and logic live in [MainViewModel].
 */
class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val vm: MainViewModel by viewModels()
    private var nfc: NfcAdapter? = null
    private lateinit var feedback: Feedback

    /** The system broadcasts NFC on/off changes; registered only while in the foreground, so it costs nothing. */
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshNfc()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        feedback = Feedback(this)
        nfc = NfcAdapter.getDefaultAdapter(this)

        val actions = AppActions(
            openCard = vm::openCard,
            back = vm::back,
            openSettings = vm::openSettings,
            setLanguage = { tag -> AppLanguage.set(this, tag) },
            rename = vm::rename,
            delete = vm::delete,
            pickFace = vm::pickFace,
            confirmCrop = vm::confirmCrop,
            cancelCrop = vm::cancelCrop,
            clearFace = vm::clearFace,
            dismissError = vm::dismissError,
            openNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
        )
        setContent { AppTheme { App(vm, actions) } }

        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(nfcStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(nfcStateReceiver, filter)
        }
        refreshNfc()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(nfcStateReceiver)
        nfc?.disableReaderMode(this)
    }

    private fun refreshNfc() {
        val adapter = nfc
        val status = when {
            adapter == null -> NfcStatus.UNSUPPORTED
            !adapter.isEnabled -> NfcStatus.DISABLED
            else -> NfcStatus.READY
        }
        vm.setNfcState(status)
        if (status == NfcStatus.READY) {
            // Reader mode has to be re-entered after NFC is switched back on.
            adapter?.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000) },
            )
        }
    }

    /** A card that launched the app from the background ("tap to open"). */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return
        Thread { onTagDiscovered(tag) }.start()
    }

    /** Called on an NFC binder thread, so blocking I/O is fine here. */
    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread { vm.onReadStarted() }
        val result = try {
            Result.success(FelicaReader.read(tag))
        } catch (e: CardReadException) {
            Result.failure(e)
        } catch (_: Exception) {
            Result.failure(CardReadException(R.string.err_read_failed))
        }
        runOnUiThread {
            vm.onReadFinished(result)
            if (result.isSuccess) feedback.success() else feedback.failure()
        }
    }
}
