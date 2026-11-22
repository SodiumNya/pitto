package app.jpcard.reader.nfc

import androidx.annotation.StringRes
import app.jpcard.reader.R

/** The result of reading a card once. */
data class CardData(
    val idm: String,
    val type: CardType,
    val balance: Int,
    val history: List<HistoryEntry>,
    val readAt: Long,
)

/** One history record (one block of service 0x090F). */
data class HistoryEntry(
    val terminal: Int,
    val process: Int,
    val year: Int,
    val month: Int,
    val day: Int,
    /** Only purchase records carry a time; -1 otherwise. */
    val hour: Int,
    val minute: Int,
    val inLine: Int,
    val inStation: Int,
    val outLine: Int,
    val outStation: Int,
    val balance: Int,
    /** Change from the previous record (positive for top-ups, negative for spending); null when it cannot be computed. */
    val delta: Int?,
    val serial: Int,
    val region: Int,
) {
    val kind: Kind get() = CardParser.kindOf(this)

    /** Area codes from byte 15: bits 7-6 are the entry area, bits 5-4 the exit area. They depend on where the ride happened, not on the card type. */
    val inArea: Int get() = (region shr 6) and 0x03
    val outArea: Int get() = (region shr 4) and 0x03

    /**
     * Record category, shown as a colored badge in the history list.
     * OTHER is the fallback for every process type that is not classified yet.
     * To split a new category out of OTHER: add an entry here (plus kind_/badge_ strings in tools/gen_strings.py), a rule in CardParser.kindOf and a color in kindColor.
     */
    enum class Kind(@get:StringRes val label: Int, @get:StringRes val badge: Int) {
        TRAIN(R.string.kind_train, R.string.badge_train),
        BUS(R.string.kind_bus, R.string.badge_bus),
        SHOPPING(R.string.kind_shopping, R.string.badge_shopping),
        CHARGE(R.string.kind_charge, R.string.badge_charge),
        OTHER(R.string.kind_other, R.string.badge_other),
    }
}
