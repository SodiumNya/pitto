package app.jpcard.reader.data

import android.content.Context
import app.jpcard.reader.nfc.CardData
import app.jpcard.reader.nfc.CardType
import app.jpcard.reader.nfc.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

/** 本地保存过的卡片（只存在本机，不联网）。 */
data class SavedCard(
    val idm: String,
    val name: String,
    val data: CardData,
)

class CardStore(context: Context) {
    private val prefs = context.getSharedPreferences("cards", Context.MODE_PRIVATE)

    fun all(): List<SavedCard> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList()).sortedByDescending { it.data.readAt }
    }

    /**
     * 保存读卡结果。名字规则：
     * - 用户改过的名字：保留
     * - 已经是本卡种的默认名（“Suica”“Suica 2”…）且没和别的卡重名：保留，名字不会跳来跳去
     * - 其他情况（新卡、旧版本的“交通卡 · XXXX”、卡种刚识别出来）：分配下一个空闲的默认名
     */
    fun save(data: CardData): SavedCard {
        val cards = all().toMutableList()
        val old = cards.find { it.idm == data.idm }
        cards.removeAll { it.idm == data.idm }
        val taken = cards.map { it.name }.toSet()
        val name = when {
            old == null -> nextDefaultName(data.type, taken)
            !isDefaultName(old.name, old.idm) -> old.name
            isSchemeName(old.name, data.type) && old.name !in taken -> old.name
            else -> nextDefaultName(data.type, taken)
        }
        val card = SavedCard(data.idm, name, data)
        cards.add(0, card)
        write(cards)
        return card
    }

    fun rename(idm: String, name: String) {
        val cards = all()
        val taken = cards.filter { it.idm != idm }.map { it.name }.toSet()
        write(cards.map {
            if (it.idm == idm) it.copy(name = name.ifBlank { nextDefaultName(it.data.type, taken) }) else it
        })
    }

    fun delete(idm: String) {
        write(all().filterNot { it.idm == idm })
    }

    private fun write(cards: List<SavedCard>) {
        val arr = JSONArray()
        cards.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "cards_v1"

        /** 第一张叫“Suica”，同种的第二张起叫“Suica 2”“Suica 3”… */
        fun nextDefaultName(type: CardType, taken: Set<String>): String {
            if (type.label !in taken) return type.label
            var n = 2
            while ("${type.label} $n" in taken) n++
            return "${type.label} $n"
        }

        private fun isSchemeName(name: String, type: CardType): Boolean {
            if (name == type.label) return true
            val suffix = name.removePrefix(type.label + " ")
            return suffix != name && suffix.toIntOrNull()?.let { it >= 2 } == true
        }

        /** 默认名（含旧版本的“交通卡 · XXXX”“Suica · XXXX”）都视为用户没改过。 */
        private fun isDefaultName(name: String, idm: String): Boolean {
            val tail = " · ${idm.takeLast(4)}"
            return name == "交通卡$tail" ||
                CardType.entries.any { name == it.label + tail || isSchemeName(name, it) }
        }

        private val FIELDS = listOf(
            "terminal", "process", "year", "month", "day", "hour", "minute",
            "inLine", "inStation", "outLine", "outStation", "balance", "serial", "region",
        )

        private fun toJson(c: SavedCard) = JSONObject().apply {
            put("idm", c.idm)
            put("name", c.name)
            put("type", c.data.type.name)
            put("balance", c.data.balance)
            put("readAt", c.data.readAt)
            put("history", JSONArray().apply {
                c.data.history.forEach { e ->
                    val v = listOf(
                        e.terminal, e.process, e.year, e.month, e.day, e.hour, e.minute,
                        e.inLine, e.inStation, e.outLine, e.outStation, e.balance, e.serial, e.region,
                    )
                    put(JSONObject().apply {
                        FIELDS.forEachIndexed { i, k -> put(k, v[i]) }
                        e.delta?.let { put("delta", it) }
                    })
                }
            })
        }

        private fun fromJson(o: JSONObject): SavedCard {
            val h = o.getJSONArray("history")
            val history = List(h.length()) { i ->
                val e = h.getJSONObject(i)
                HistoryEntry(
                    terminal = e.getInt("terminal"),
                    process = e.getInt("process"),
                    year = e.getInt("year"),
                    month = e.getInt("month"),
                    day = e.getInt("day"),
                    hour = e.getInt("hour"),
                    minute = e.getInt("minute"),
                    inLine = e.getInt("inLine"),
                    inStation = e.getInt("inStation"),
                    outLine = e.getInt("outLine"),
                    outStation = e.getInt("outStation"),
                    balance = e.getInt("balance"),
                    delta = if (e.has("delta")) e.getInt("delta") else null,
                    serial = e.getInt("serial"),
                    region = e.getInt("region"),
                )
            }.filter { it.month != 0 } // 旧版本可能存了空记录位
            val idm = o.getString("idm")
            val type = CardType.fromName(o.optString("type"))
            return SavedCard(
                idm = idm,
                name = o.optString("name", type.label),
                data = CardData(idm, type, o.getInt("balance"), history, o.getLong("readAt")),
            )
        }
    }
}
