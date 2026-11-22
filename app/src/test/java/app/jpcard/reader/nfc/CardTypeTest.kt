package app.jpcard.reader.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTypeTest {

    private fun probesFor(vararg types: CardType): BooleanArray {
        val codes = CardType.allProbes
        return BooleanArray(codes.size) { i -> types.any { codes[i] in it.probes } }
    }

    @Test
    fun detectsASingleIssuer() {
        assertEquals(CardType.SUICA, CardType.fromProbeResult(probesFor(CardType.SUICA)))
        assertEquals(CardType.ICOCA, CardType.fromProbeResult(probesFor(CardType.ICOCA)))
    }

    @Test
    fun ambiguousOrMissingIsUnknown() {
        assertEquals(CardType.UNKNOWN, CardType.fromProbeResult(probesFor(CardType.SUICA, CardType.PASMO)))
        assertEquals(CardType.UNKNOWN, CardType.fromProbeResult(probesFor()))
    }

    @Test
    fun probeCodesAreUnique() {
        assertEquals(CardType.allProbes.size, CardType.allProbes.toSet().size)
    }
}
