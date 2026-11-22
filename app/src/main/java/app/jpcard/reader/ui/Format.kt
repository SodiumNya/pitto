package app.jpcard.reader.ui

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

fun readTime(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val pattern = when {
        now.get(Calendar.YEAR) != then.get(Calendar.YEAR) -> "yyyy/MM/dd HH:mm"
        now.get(Calendar.DAY_OF_YEAR) != then.get(Calendar.DAY_OF_YEAR) -> "MM/dd HH:mm"
        else -> "今天 HH:mm"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

fun entryDate(e: HistoryEntry): String {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val date = if (e.year == year) "%d/%d".format(e.month, e.day)
    else "%d/%d/%d".format(e.year, e.month, e.day)
    return if (e.hour >= 0) "%s %02d:%02d".format(date, e.hour, e.minute) else date
}

fun entryTitle(e: HistoryEntry): String = CardParser.processLabel(e.process)

/** 查不到站名时退回显示 线区-站 编号。 */
private fun stationName(area: Int, line: Int, station: Int): String =
    Stations.find(area, line, station)?.name ?: "%d-%d".format(line, station)

fun entryDetail(e: HistoryEntry): String? = when (e.kind) {
    HistoryEntry.Kind.TRAIN -> {
        val from = stationName(e.inArea, e.inLine, e.inStation)
        if (e.outLine == 0 && e.outStation == 0) {
            from
        } else {
            "$from → " + stationName(e.outArea, e.outLine, e.outStation)
        }
    }
    else -> null
}
