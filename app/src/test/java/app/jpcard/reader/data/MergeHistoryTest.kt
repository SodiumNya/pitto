package app.jpcard.reader.data

import app.jpcard.reader.nfc.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MergeHistoryTest {

    private fun entry(serial: Int, balance: Int, delta: Int?) = HistoryEntry(
        terminal = 0x16, process = 0x01, year = 2026, month = 11, day = serial % 28 + 1,
        hour = -1, minute = -1, inLine = 0, inStation = 0, outLine = 0, outStation = 0,
        balance = balance, delta = delta, serial = serial, region = 0,
    )

    @Test
    fun keepsOlderRecordsTheCardNoLongerHolds() {
        val saved = listOf(entry(3, 300, -10), entry(2, 310, -10), entry(1, 320, null))
        val fresh = listOf(entry(5, 280, -10), entry(4, 290, -10), entry(3, 300, null))
        val merged = CardStore.mergeHistory(fresh, saved)
        assertEquals(listOf(5, 4, 3, 2, 1), merged.map { it.serial })
    }

    @Test
    fun prefersTheCopyWithAKnownAmount() {
        // The oldest record of a read has no amount; an earlier read already worked it out.
        val saved = listOf(entry(3, 300, -10))
        val fresh = listOf(entry(4, 290, -10), entry(3, 300, null))
        assertEquals(-10, CardStore.mergeHistory(fresh, saved).first { it.serial == 3 }.delta)
    }

    @Test
    fun freshCopyWinsWhenBothHaveAmounts() {
        val saved = listOf(entry(3, 300, -99))
        val fresh = listOf(entry(3, 300, -10))
        assertEquals(-10, CardStore.mergeHistory(fresh, saved).single().delta)
    }

    @Test
    fun isCapped() {
        val saved = (1..1200).map { entry(it, 1000, -1) }
        val merged = CardStore.mergeHistory(emptyList(), saved)
        assertEquals(1000, merged.size)
        assertEquals(1200, merged.first().serial) // the newest are kept
    }
}
