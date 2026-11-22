package app.jpcard.reader.nfc

import app.jpcard.reader.nfc.HistoryEntry.Kind

/**
 * CJRC 格式（Suica / ICOCA 等交通系 IC 卡通用）的数据解析。
 *
 * 乘车记录块（16 字节）：
 *  [0] 终端种类  [1] 处理种类  [2] 支付种类  [3] 进出站种类
 *  [4-5] 日期：年(7bit, 2000+) 月(4bit) 日(5bit)
 *  [6-7] 进站 线区/站  （消费类为 时(5) 分(6) 秒(5)）
 *  [8-9] 出站 线区/站
 *  [10-11] 余额（小端序）
 *  [12-14] 流水号  [15] 地区
 */
object CardParser {

    private val SHOPPING = setOf(0x46, 0x49, 0x4A, 0x4B, 0xC6, 0xCB)
    private val BUS = setOf(0x0D, 0x0F, 0x1F, 0x23)
    private val CHARGE = setOf(0x02, 0x14, 0x15, 0x1F, 0x48, 0x49)

    fun parse(idm: ByteArray, type: CardType, blocks: List<ByteArray>, attribute: ByteArray?): CardData {
        // 空的记录位不一定全是 0，以日期字段为空作为判断依据
        val raw = blocks.filter { be16(it, 4) != 0 }.map(::parseBlock)
        val entries = raw.mapIndexed { i, e ->
            val older = raw.getOrNull(i + 1)
            val delta = when {
                older != null -> e.balance - older.balance
                e.process == 0x07 -> e.balance // 新卡发行时的初始余额
                else -> null
            }
            e.copy(delta = delta)
        }
        val balance = entries.firstOrNull()?.balance
            ?: attribute?.let { le16(it, 11) }
            ?: 0
        return CardData(
            idm = idm.joinToString("") { "%02X".format(it) },
            type = type,
            balance = balance,
            history = entries,
            readAt = System.currentTimeMillis(),
        )
    }

    private fun parseBlock(b: ByteArray): HistoryEntry {
        val process = u8(b, 1)
        val date = be16(b, 4)
        val shopping = process in SHOPPING
        val time = be16(b, 6)
        return HistoryEntry(
            terminal = u8(b, 0),
            process = process,
            year = 2000 + (date shr 9),
            month = (date shr 5) and 0x0F,
            day = date and 0x1F,
            hour = if (shopping) time shr 11 else -1,
            minute = if (shopping) (time shr 5) and 0x3F else -1,
            inLine = u8(b, 6),
            inStation = u8(b, 7),
            outLine = u8(b, 8),
            outStation = u8(b, 9),
            balance = le16(b, 10),
            delta = null,
            serial = (u8(b, 12) shl 16) or (u8(b, 13) shl 8) or u8(b, 14),
            region = u8(b, 15),
        )
    }

    fun kindOf(e: HistoryEntry): Kind = when {
        e.process in CHARGE -> Kind.CHARGE
        e.process in SHOPPING -> Kind.SHOPPING
        e.process in BUS || e.terminal == 0x05 -> Kind.BUS
        e.process in setOf(0x01, 0x03, 0x04, 0x05, 0x06, 0x13, 0x84, 0x85) -> Kind.TRAIN
        else -> Kind.OTHER
    }

    fun processLabel(process: Int): String = PROCESS[process] ?: PROCESS[process and 0x7F] ?: "其他"

    private val PROCESS = mapOf(
        0x01 to "乘车",
        0x02 to "充值",
        0x03 to "购票",
        0x04 to "补票",
        0x05 to "进站补票",
        0x06 to "窗口处理",
        0x07 to "新卡",
        0x08 to "扣除",
        0x0D to "巴士",
        0x0F to "巴士",
        0x11 to "补发卡",
        0x13 to "新干线",
        0x14 to "自动充值",
        0x15 to "自动充值",
        0x1F to "巴士充值",
        0x23 to "巴士购票",
        0x46 to "购物",
        0x48 to "积分充值",
        0x49 to "收银台充值",
        0x4A to "购物取消",
        0x4B to "站内购物",
        0x84 to "补票（他社）",
        0x85 to "进站补票（他社）",
        0xC6 to "购物",
        0xCB to "站内购物",
    )

    private fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    private fun be16(b: ByteArray, i: Int) = (u8(b, i) shl 8) or u8(b, i + 1)
    private fun le16(b: ByteArray, i: Int) = u8(b, i) or (u8(b, i + 1) shl 8)
}
