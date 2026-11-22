package app.jpcard.reader.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import androidx.annotation.StringRes
import app.jpcard.reader.R
import java.io.ByteArrayOutputStream
import java.io.IOException

/** A read failure with a user-facing message (string resource). */
class CardReadException(@get:StringRes val reason: Int) : IOException()

private class FelicaStatusException(val status1: Int, val status2: Int) :
    IOException("FeliCa status %02X %02X".format(status1, status2))

/**
 * Reads Japanese transit IC cards (Suica, ICOCA, PASMO, ...; all CJRC format).
 * Balance and history are services readable without encryption, so no keys are involved.
 */
object FelicaReader {
    private const val SYSTEM_CJRC = 0x0003
    private const val SERVICE_HISTORY = 0x090F
    private const val SERVICE_ATTRIBUTE = 0x008B
    private const val HISTORY_BLOCKS = 20
    private const val BLOCK_SIZE = 16

    fun read(tag: Tag): CardData {
        val nfcF = NfcF.get(tag) ?: throw CardReadException(R.string.err_not_felica)
        try {
            nfcF.connect()
            nfcF.timeout = 1500
            val idm = poll(nfcF, SYSTEM_CJRC) ?: throw CardReadException(R.string.err_not_transit)
            val type = try {
                detectType(nfcF, idm)
            } catch (e: TagLostException) {
                throw e
            } catch (_: IOException) {
                CardType.UNKNOWN
            }
            val history = readBlocks(nfcF, idm, SERVICE_HISTORY, HISTORY_BLOCKS)
            val attribute = try {
                readBlocks(nfcF, idm, SERVICE_ATTRIBUTE, 1).firstOrNull()
            } catch (e: CardReadException) {
                throw e
            } catch (_: IOException) {
                null
            }
            return CardParser.parse(idm, type, history, attribute)
        } catch (_: TagLostException) {
            throw CardReadException(R.string.err_tag_lost)
        } catch (e: CardReadException) {
            throw e
        } catch (_: IOException) {
            throw CardReadException(R.string.err_read_failed)
        } finally {
            runCatching { nfcF.close() }
        }
    }

    /** Polls for a system code and returns that system's IDm, or null if the card has no such system. */
    private fun poll(f: NfcF, systemCode: Int): ByteArray? {
        val cmd = byteArrayOf(
            6, 0x00,
            (systemCode shr 8).toByte(), systemCode.toByte(),
            0x01, // request code: system code
            0x00, // time slot
        )
        val res = try {
            f.transceive(cmd)
        } catch (_: TagLostException) {
            // When the card does not answer for this system code, most phones report TagLost.
            return null
        }
        if (res.size < 18 || res[1] != 0x01.toByte()) return null
        return res.copyOfRange(2, 10)
    }

    /**
     * Request Service (0x02): asks whether several service codes exist in one go.
     * Missing services report key version 0xFFFF.
     */
    private fun detectType(f: NfcF, idm: ByteArray): CardType {
        val codes = CardType.allProbes
        val buf = ByteArrayOutputStream()
        buf.write(0)
        buf.write(0x02)
        buf.write(idm)
        buf.write(codes.size)
        for (c in codes) {
            buf.write(c and 0xFF)
            buf.write((c shr 8) and 0xFF)
        }
        val cmd = buf.toByteArray()
        cmd[0] = cmd.size.toByte()

        val res = f.transceive(cmd)
        if (res.size < 11 + codes.size * 2 || res[1] != 0x03.toByte()) return CardType.UNKNOWN
        val exists = BooleanArray(codes.size) { i ->
            val lo = res[11 + i * 2].toInt() and 0xFF
            val hi = res[12 + i * 2].toInt() and 0xFF
            (hi shl 8 or lo) != 0xFFFF
        }
        return CardType.fromProbeResult(exists)
    }

    private fun readBlocks(f: NfcF, idm: ByteArray, service: Int, count: Int): List<ByteArray> {
        val out = ArrayList<ByteArray>(count)
        var chunk = 10
        var start = 0
        while (start < count) {
            val n = minOf(chunk, count - start)
            try {
                out += readWithoutEncryption(f, idm, service, start, n)
                start += n
            } catch (_: FelicaStatusException) {
                // Multi-block reads are unsupported, or we read past the last block.
                if (n > 1) chunk = 1 else break
            }
        }
        return out
    }

    private fun readWithoutEncryption(
        f: NfcF, idm: ByteArray, service: Int, start: Int, n: Int,
    ): List<ByteArray> {
        val buf = ByteArrayOutputStream()
        buf.write(0) // length, filled in below
        buf.write(0x06)
        buf.write(idm)
        buf.write(1) // number of services
        buf.write(service and 0xFF) // service code, little-endian
        buf.write((service shr 8) and 0xFF)
        buf.write(n)
        for (i in 0 until n) {
            buf.write(0x80) // 2-byte block list element
            buf.write(start + i)
        }
        val cmd = buf.toByteArray()
        cmd[0] = cmd.size.toByte()

        val res = f.transceive(cmd)
        if (res.size < 12 || res[1] != 0x07.toByte()) throw IOException("unexpected response")
        val s1 = res[10].toInt() and 0xFF
        val s2 = res[11].toInt() and 0xFF
        if (s1 != 0) throw FelicaStatusException(s1, s2)
        val num = res[12].toInt() and 0xFF
        if (res.size < 13 + num * BLOCK_SIZE) throw IOException("short response")
        return List(num) { i -> res.copyOfRange(13 + i * BLOCK_SIZE, 13 + (i + 1) * BLOCK_SIZE) }
    }
}
