package app.jpcard.reader.nfc

/** 一次读卡的结果。 */
data class CardData(
    val idm: String,
    val type: CardType,
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

    /** 地区码：第 15 字节高 2 位为进站地区，接下来 2 位为出站地区（与卡种无关，取决于乘车地点）。 */
    val inArea: Int get() = (region shr 6) and 0x03
    val outArea: Int get() = (region shr 4) and 0x03

    /**
     * 记录类别，用于在列表里用颜色区分。
     * OTHER 是兜底：没被归类的处理种类都落在这里。
     * 想从 OTHER 里拆出新类别时：这里加一项 → CardParser.kindOf 加一条规则 → theme 里 kindColor 加一种颜色。
     */
    enum class Kind(val label: String) {
        TRAIN("乘车"),
        BUS("巴士"),
        SHOPPING("购物"),
        CHARGE("充值"),
        OTHER("其他"),
    }
}
