package app.jpcard.reader.nfc

import app.jpcard.reader.nfc.HistoryEntry.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardParserTest {

    /** Builds a 16-byte history block in the CJRC layout. */
    private fun block(
        terminal: Int = 0x16,
        process: Int = 0x01,
        year: Int = 2026, month: Int = 11, day: Int = 9,
        b6: Int = 0, b7: Int = 0, b8: Int = 0, b9: Int = 0,
        balance: Int,
        serial: Int,
        region: Int = 0,
    ): ByteArray {
        val date = ((year - 2000) shl 9) or (month shl 5) or day
        return byteArrayOf(
            terminal.toByte(), process.toByte(), 0, 0,
            (date shr 8).toByte(), date.toByte(),
            b6.toByte(), b7.toByte(), b8.toByte(), b9.toByte(),
            balance.toByte(), (balance shr 8).toByte(),
            (serial shr 16).toByte(), (serial shr 8).toByte(), serial.toByte(),
            region.toByte(),
        )
    }

    private val idm = ByteArray(8) { it.toByte() }

    @Test
    fun parsesBalanceDatesAndDeltas() {
        val data = CardParser.parse(
            idm, CardType.ICOCA,
            listOf(
                block(balance = 63, serial = 12),   // newest
                block(balance = 273, serial = 11),
                block(balance = 513, serial = 10),  // oldest on the card
            ),
            attribute = null,
        )
        assertEquals(63, data.balance)
        assertEquals("0001020304050607", data.idm)
        assertEquals(listOf(-210, -240, null), data.history.map { it.delta })
        with(data.history.first()) {
            assertEquals(2026, year); assertEquals(11, month); assertEquals(9, day)
            assertEquals(-1, hour)
        }
    }

    @Test
    fun skipsEmptySlotsEvenWhenNotAllZero() {
        val empty = ByteArray(16).also { it[15] = 0x40 } // no date, but not all zeros
        val data = CardParser.parse(idm, CardType.SUICA, listOf(block(balance = 500, serial = 1, process = 0x07), empty), null)
        assertEquals(1, data.history.size)
    }

    @Test
    fun newCardRecordUsesItsInitialBalanceAsAmount() {
        val data = CardParser.parse(idm, CardType.SUICA, listOf(block(process = 0x07, terminal = 0x18, balance = 500, serial = 1)), null)
        assertEquals(500, data.history.single().delta)
    }

    @Test
    fun fallsBackToAttributeBalanceWithoutHistory() {
        val attribute = ByteArray(16).also { it[11] = 0xE8.toByte(); it[12] = 0x03 } // 1000, little-endian
        assertEquals(1000, CardParser.parse(idm, CardType.SUICA, emptyList(), attribute).balance)
    }

    @Test
    fun classifiesRecords() {
        fun kind(terminal: Int, process: Int) =
            CardParser.parse(idm, CardType.UNKNOWN, listOf(block(terminal = terminal, process = process, balance = 0, serial = 1)), null)
                .history.single().kind
        assertEquals(Kind.TRAIN, kind(0x16, 0x01))
        assertEquals(Kind.BUS, kind(0x05, 0x0F))
        assertEquals(Kind.CHARGE, kind(0xC8, 0x49))
        assertEquals(Kind.SHOPPING, kind(0xC7, 0x46))
        assertEquals(Kind.OTHER, kind(0x18, 0x07))
    }

    @Test
    fun readsPurchaseTime() {
        val time = (16 shl 11) or (31 shl 5) // 16:31
        val e = CardParser.parse(
            idm, CardType.UNKNOWN,
            listOf(block(terminal = 0xC8, process = 0x46, b6 = time shr 8, b7 = time and 0xFF, balance = 0, serial = 1)),
            null,
        ).history.single()
        assertEquals(16, e.hour)
        assertEquals(31, e.minute)
    }

    @Test
    fun readsEntryAndExitAreas() {
        val e = CardParser.parse(idm, CardType.UNKNOWN, listOf(block(region = 0x50, balance = 0, serial = 1)), null).history.single()
        assertEquals(1, e.inArea)
        assertEquals(1, e.outArea)
        assertNull(e.delta)
    }
}
