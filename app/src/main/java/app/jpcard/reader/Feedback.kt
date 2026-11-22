package app.jpcard.reader

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * 读卡结果反馈：提示音 + 震动。
 * 提示音在代码里合成，不占安装包体积；手机处于静音/震动模式时只震动。
 */
class Feedback(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }
    private val handler = Handler(Looper.getMainLooper())

    // 成功：上行的两个清脆音（E6 → A6），失败：下行的两个低音
    private val successPcm by lazy { chime(listOf(1318.5 to 0.075, 1760.0 to 0.16)) }
    private val failurePcm by lazy { chime(listOf(523.3 to 0.09, 392.0 to 0.16)) }

    fun success() {
        play(successPcm)
        vibrate(longArrayOf(0, 45))
    }

    fun failure() {
        play(failurePcm)
        vibrate(longArrayOf(0, 35, 90, 35))
    }

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun play(pcm: ShortArray) {
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            val ms = pcm.size * 1000L / RATE + 200
            handler.postDelayed({ runCatching { track.release() } }, ms)
        }
    }

    private companion object {
        const val RATE = 44100

        /** 合成几个依次响起的音：带 3ms 起音和指数衰减，外加一点二次谐波让声音更圆润。 */
        fun chime(notes: List<Pair<Double, Double>>): ShortArray {
            val tail = 0.12
            val total = notes.sumOf { it.second } + tail
            val out = FloatArray((total * RATE).toInt())
            var start = 0.0
            for ((freq, len) in notes) {
                val begin = (start * RATE).toInt()
                val n = ((len + tail) * RATE).toInt()
                for (i in 0 until min(n, out.size - begin)) {
                    val t = i.toDouble() / RATE
                    val env = min(1.0, t / 0.003) * exp(-t / (len * 0.55))
                    val s = sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t)
                    out[begin + i] += (s * env).toFloat()
                }
                start += len
            }
            return ShortArray(out.size) { i ->
                (out[i].coerceIn(-1.25f, 1.25f) * 0.28f * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
