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
import app.jpcard.reader.data.CardFaces
import app.jpcard.reader.data.CardStore
import app.jpcard.reader.data.Stations
import app.jpcard.reader.nfc.CardReadException
import app.jpcard.reader.nfc.FelicaReader
import app.jpcard.reader.ui.App
import app.jpcard.reader.ui.AppActions
import app.jpcard.reader.ui.AppState
import app.jpcard.reader.ui.CropRequest
import app.jpcard.reader.ui.NfcStatus
import app.jpcard.reader.ui.theme.AppTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfc: NfcAdapter? = null
    private lateinit var store: CardStore
    private lateinit var feedback: Feedback
    private val state = AppState()

    /** 系统在 NFC 开关变化时发广播，只在前台时注册，不轮询、不耗电。 */
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshNfc()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = CardStore(this)
        feedback = Feedback(this)
        nfc = NfcAdapter.getDefaultAdapter(this)
        state.cards = store.all()
        state.stationsReady = Stations.loaded
        if (!Stations.loaded) {
            Thread {
                runCatching { Stations.load(applicationContext) }
                runOnUiThread { state.stationsReady = Stations.loaded }
            }.start()
        }

        val actions = AppActions(
            openCard = { state.selectedIdm = it },
            back = { state.selectedIdm = null },
            rename = { idm, name -> store.rename(idm, name); state.cards = store.all() },
            delete = { idm ->
                store.delete(idm)
                CardFaces.delete(this, idm)
                state.cards = store.all()
                if (state.selectedIdm == idm) state.selectedIdm = null
            },
            setFace = { idm, uri ->
                // 先解码成适合编辑的大小，再打开裁剪界面
                state.faceBusy = true
                Thread {
                    val bmp = CardFaces.decodeForEdit(applicationContext, uri)
                    runOnUiThread {
                        state.faceBusy = false
                        if (bmp == null) {
                            state.error = "无法读取这张图片，请换一张"
                        } else {
                            state.crop = CropRequest(idm, bmp)
                        }
                    }
                }.start()
            },
            confirmCrop = { bmp, rect ->
                val idm = state.crop?.idm
                state.crop = null
                if (idm != null) {
                    state.faceBusy = true
                    Thread {
                        val ok = CardFaces.save(applicationContext, idm, bmp, rect)
                        runOnUiThread {
                            state.faceBusy = false
                            state.faceVersion++
                            if (!ok) state.error = "卡面保存失败，请重试"
                        }
                    }.start()
                }
            },
            cancelCrop = { state.crop = null },
            clearFace = { idm -> CardFaces.delete(this, idm); state.faceVersion++ },
            dismissError = { state.error = null },
            openNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
        )
        setContent { AppTheme { App(state, actions) } }

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
        state.nfcStatus = status
        if (status == NfcStatus.READY) {
            // NFC 重新打开后需要重新进入读卡模式
            adapter?.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000) },
            )
        } else {
            state.reading = false
        }
    }

    /** 冷启动时由“贴卡自动打开”带进来的卡片。 */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return
        Thread { onTagDiscovered(tag) }.start()
    }

    /** NFC 线程回调，可直接做阻塞 IO。 */
    override fun onTagDiscovered(tag: Tag) {
        runOnUiThread { state.reading = true; state.error = null }
        val result = try {
            Result.success(FelicaReader.read(tag))
        } catch (e: CardReadException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CardReadException("读取失败，请将卡片贴稳后重试"))
        }
        runOnUiThread {
            state.reading = false
            result.onSuccess { data ->
                val card = store.save(data)
                state.cards = store.all()
                state.selectedIdm = card.idm
                state.celebrate(card.idm)
                feedback.success()
            }.onFailure {
                state.error = it.message
                state.errorTick++
                feedback.failure()
            }
        }
    }
}
