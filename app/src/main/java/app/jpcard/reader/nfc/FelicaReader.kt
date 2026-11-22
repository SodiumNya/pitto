package app.jpcard.reader.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import java.io.ByteArrayOutputStream
import java.io.IOException

class CardReadException(message: String) : IOException(message)

private class FelicaStatusException(val status1: Int, val status2: Int) :
    IOException("FeliCa status %02X %02X".format(status1, status2))

/**
 * 读取交通系 IC 卡（Suica / ICOCA / PASMO 等，均为 CJRC 格式）。
 * 余额与乘车记录是“无需加密即可读取”的服务，不涉及任何密钥。
 */
object FelicaReader {
    private const val SYSTEM_CJRC = 0x0003
    private const val SERVICE_HISTORY = 0x090F
    private const val SERVICE_ATTRIBUTE = 0x008B
    private const val HISTORY_BLOCKS = 20
    private const val BLOCK_SIZE = 16

    fun read(tag: Tag): CardData {
        val nfcF = NfcF.get(tag) ?: throw CardReadException("不是 FeliCa 卡")
        try {
            nfcF.connect()
            nfcF.timeout = 1500
            val idm = poll(nfcF, SYSTEM_CJRC) ?: throw CardReadException("不是交通系 IC 卡")
            val history = readBlocks(nfcF, idm, SERVICE_HISTORY, HISTORY_BLOCKS)
            val attribute = try {
                readBlocks(nfcF, idm, SERVICE_ATTRIBUTE, 1).firstOrNull()
            } catch (e: CardReadException) {
                throw e
            } catch (_: IOException) {
                null
            }
            return CardParser.parse(idm, history, attribute)
        } catch (e: TagLostException) {
            throw CardReadException("卡片离开了，请贴住不动再试一次")
        } catch (e: CardReadException) {
            throw e
        } catch (e: IOException) {
            throw CardReadException("读取失败，请贴住不动再试一次")
        } finally {
            runCatching { nfcF.close() }
        }
    }

    /** Polling 指定系统码，返回该系统的 IDm；卡上没有该系统时返回 null。 */
    private fun poll(f: NfcF, systemCode: Int): ByteArray? {
        val cmd = byteArrayOf(
            6, 0x00,
            (systemCode shr 8).toByte(), systemCode.toByte(),
            0x01, // 请求系统码
            0x00, // 时隙
        )
        val res = try {
            f.transceive(cmd)
        } catch (_: TagLostException) {
            // 卡片不响应该系统码时，多数手机会表现为 TagLost
            return null
        }
        if (res.size < 18 || res[1] != 0x01.toByte()) return null
        return res.copyOfRange(2, 10)
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
            } catch (e: FelicaStatusException) {
                // 一次读多块不被支持，或读到了不存在的块
                if (n > 1) chunk = 1 else break
            }
        }
        return out
    }

    private fun readWithoutEncryption(
        f: NfcF, idm: ByteArray, service: Int, start: Int, n: Int,
    ): List<ByteArray> {
        val buf = ByteArrayOutputStream()
        buf.write(0) // 长度占位
        buf.write(0x06)
        buf.write(idm)
        buf.write(1) // 服务数
        buf.write(service and 0xFF) // 服务码小端序
        buf.write((service shr 8) and 0xFF)
        buf.write(n)
        for (i in 0 until n) {
            buf.write(0x80) // 2 字节块列表元素
            buf.write(start + i)
        }
        val cmd = buf.toByteArray()
        cmd[0] = cmd.size.toByte()

        val res = f.transceive(cmd)
        if (res.size < 12 || res[1] != 0x07.toByte()) throw IOException("响应格式异常")
        val s1 = res[10].toInt() and 0xFF
        val s2 = res[11].toInt() and 0xFF
        if (s1 != 0) throw FelicaStatusException(s1, s2)
        val num = res[12].toInt() and 0xFF
        if (res.size < 13 + num * BLOCK_SIZE) throw IOException("响应长度异常")
        return List(num) { i -> res.copyOfRange(13 + i * BLOCK_SIZE, 13 + (i + 1) * BLOCK_SIZE) }
    }
}
