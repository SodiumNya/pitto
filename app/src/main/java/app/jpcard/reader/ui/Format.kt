package app.jpcard.reader.ui

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.jpcard.reader.R
import app.jpcard.reader.data.Stations
import app.jpcard.reader.nfc.CardParser
import app.jpcard.reader.nfc.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun yen(amount: Int): String = "¥" + "%,d".format(Locale.JAPAN, amount)

fun signedYen(amount: Int): String = when {
    amount > 0 -> "+" + yen(amount)
    amount < 0 -> "−" + yen(-amount)
    else -> yen(0)
}

/** Formats with the current locale's pattern, e.g. "11/22 14:30", "Nov 22 14:30", "11月22日 14:30". */
private fun localized(skeleton: String, time: Date): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(time)
}

/** Read time: "Today 14:30" for today, month/day this year, full date otherwise. */
@Composable
fun readTime(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val time = Date(millis)
    return when {
        now.get(Calendar.YEAR) != then.get(Calendar.YEAR) -> localized("yMdHm", time)
        now.get(Calendar.DAY_OF_YEAR) != then.get(Calendar.DAY_OF_YEAR) -> localized("MdHm", time)
        else -> stringResource(R.string.today_at, localized("Hm", time))
    }
}

fun entryDate(e: HistoryEntry): String {
    val cal = Calendar.getInstance()
    val thisYear = cal.get(Calendar.YEAR)
    cal.clear()
    cal.set(e.year, e.month - 1, e.day, e.hour.coerceAtLeast(0), e.minute.coerceAtLeast(0))
    val date = if (e.year == thisYear) "Md" else "yMd"
    return localized(if (e.hour >= 0) date + "Hm" else date, cal.time)
}

@StringRes
fun entryTitle(e: HistoryEntry): Int = CardParser.processLabel(e.process)

/** Falls back to "line-station" numbers when the station is unknown. */
private fun stationName(area: Int, line: Int, station: Int, english: Boolean): String =
    Stations.find(area, line, station)?.displayName(english) ?: "%d-%d".format(line, station)

/** Route or bus operator for a record; Japanese names unless [english] is on. */
fun entryDetail(e: HistoryEntry, english: Boolean): String? = when (e.kind) {
    HistoryEntry.Kind.TRAIN -> {
        val from = stationName(e.inArea, e.inLine, e.inStation, english)
        if (e.outLine == 0 && e.outStation == 0) {
            from
        } else {
            "$from → " + stationName(e.outArea, e.outLine, e.outStation, english)
        }
    }
    // Bus records: bytes 6-7 are the operator, bytes 8-9 the stop (a single stop, no route).
    // Stop names exist in Japanese only.
    HistoryEntry.Kind.BUS -> {
        val operator = (e.inLine shl 8) or e.inStation
        val stop = (e.outLine shl 8) or e.outStation
        listOfNotNull(Stations.busOperator(operator)?.displayName(english), Stations.busStop(operator, stop))
            .joinToString(" · ")
            .ifEmpty { null }
    }
    else -> null
}
