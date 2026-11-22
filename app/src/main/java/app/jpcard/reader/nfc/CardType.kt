package app.jpcard.reader.nfc

import android.content.Context
import app.jpcard.reader.R

/**
 * Transit IC card type.
 *
 * Every issuer registers a few service codes of its own in the CJRC system (0x0003);
 * asking which of them exist with Request Service tells the card types apart.
 * The service code lists come from Metrodroid's research (SuicaUtil.kt).
 */
enum class CardType(val label: String, val probes: IntArray) {
    SUICA("Suica", intArrayOf(0x1808, 0x18C8)),
    ICOCA("ICOCA", intArrayOf(0x1A48, 0x9608)),
    PASMO("PASMO", intArrayOf(0x1CC8, 0x1D08)),
    TOICA("TOICA", intArrayOf(0x1E08, 0x1E48)),
    MANACA("manaca", intArrayOf(0x9888, 0x9908)),
    KITACA("Kitaca", intArrayOf(0x2088, 0x2108)),
    NIMOCA("nimoca", intArrayOf(0x1F48, 0x2008)),
    PITAPA("PiTaPa", intArrayOf(0x1B88, 0x9748)),
    SUGOCA("SUGOCA", intArrayOf(0x21C8, 0x2208)),
    HAYAKAKEN("はやかけん", intArrayOf(0x2448, 0x2508)),
    UNKNOWN("IC", intArrayOf());

    companion object {
        val detectable = entries.filter { it.probes.isNotEmpty() }

        /** All probe codes, in [detectable] order (20 in total, so one command checks them all). */
        val allProbes: IntArray = detectable.flatMap { it.probes.asList() }.toIntArray()

        /** Decides the type from which probes exist; only an unambiguous single match counts. */
        fun fromProbeResult(exists: BooleanArray): CardType {
            var i = 0
            val hits = detectable.filter { t ->
                val hit = t.probes.indices.any { exists[i + it] }
                i += t.probes.size
                hit
            }
            return hits.singleOrNull() ?: UNKNOWN
        }

        fun fromName(name: String?): CardType = entries.find { it.name == name } ?: UNKNOWN
    }

    /** Display name: card names are proper nouns and not translated; only "unknown" is localized. */
    fun displayName(context: Context): String =
        if (this == UNKNOWN) context.getString(R.string.card_type_unknown) else label
}
