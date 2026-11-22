package app.jpcard.reader.nfc

/** 一次读卡的结果。 */
data class CardData(
    val idm: String,
    val balance: Int,
    val history: List<HistoryEntry>,
    val readAt: Long,
)

/** 一条乘车/消费记录（服务码 0x090F 的一个块）。 */
data class HistoryEntry(
    val terminal: Int,
    val process: Int,
    val year: Int,
    val month: Int,
    val day: Int,
    /** 仅消费类记录有时间，其他为 -1 */
    val hour: Int,
    val minute: Int,
    val inLine: Int,
    val inStation: Int,
    val outLine: Int,
    val outStation: Int,
    val balance: Int,
    /** 相对上一条的变动（充值为正、消费为负）；最旧一条无法计算时为 null */
    val delta: Int?,
    val serial: Int,
    val region: Int,
) {
    val kind: Kind get() = CardParser.kindOf(this)

    enum class Kind { TRAIN, BUS, SHOPPING, CHARGE, OTHER }
}
