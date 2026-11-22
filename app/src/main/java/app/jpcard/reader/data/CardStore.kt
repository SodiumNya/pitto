package app.jpcard.reader.data

import android.content.Context
import app.jpcard.reader.nfc.CardData
import app.jpcard.reader.nfc.CardType
import app.jpcard.reader.nfc.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

/** A card saved on this device (never uploaded anywhere). */
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
     * Saves a read result. Naming rules:
     * - a name the user changed is kept;
     * - a default name of this card type ("Suica", "Suica 2", ...) that no other card uses is kept, so names never jump around;
     * - otherwise (new card, legacy default name, type newly detected) the next free default name is assigned.
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

        /** The first card is "Suica"; further cards of the same type are "Suica 2", "Suica 3", ... */
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

        /** Default names, including legacy ones from earlier versions, count as "not renamed by the user". */
        private fun isDefaultName(name: String, idm: String): Boolean {
            val tail = " · ${idm.takeLast(4)}"
            return name == "交通卡$tail" || name.startsWith("交通系IC") ||
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
            }.filter { it.month != 0 } // older versions may have stored empty history slots
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
