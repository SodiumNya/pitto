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

    /** 保存读卡结果，保留用户改过的名字；还是默认名的就按（新识别出的）卡种更新。 */
    fun save(data: CardData): SavedCard {
        val cards = all().toMutableList()
        val old = cards.find { it.idm == data.idm }
        val keepName = old != null && !isDefaultName(old.name, old.idm)
        val name = if (keepName) old!!.name else defaultName(data.idm, data.type)
        val card = SavedCard(data.idm, name, data)
        cards.removeAll { it.idm == data.idm }
        cards.add(0, card)
        write(cards)
        return card
    }

    fun rename(idm: String, name: String) {
        write(all().map { if (it.idm == idm) it.copy(name = name.ifBlank { defaultName(idm, it.data.type) }) else it })
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

        fun defaultName(idm: String, type: CardType) = "${type.label} · ${idm.takeLast(4)}"

        /** 旧版本的默认名“交通卡 · XXXX”或任一卡种的默认名，都视为用户没改过。 */
        private fun isDefaultName(name: String, idm: String) =
            name == "交通卡 · ${idm.takeLast(4)}" || CardType.entries.any { name == defaultName(idm, it) }

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
            }
            val idm = o.getString("idm")
            val type = CardType.fromName(o.optString("type"))
            return SavedCard(
                idm = idm,
                name = o.optString("name", defaultName(idm, type)),
                data = CardData(idm, type, o.getInt("balance"), history, o.getLong("readAt")),
            )
        }
    }
}
