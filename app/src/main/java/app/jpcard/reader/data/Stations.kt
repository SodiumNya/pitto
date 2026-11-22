package app.jpcard.reader.data

import android.content.Context

data class Station(val name: String, val company: String, val line: String)

/**
 * 车站 / 巴士代码表（assets 下，由 tools/gen_stations.py 生成）。
 * - stations.tsv：铁路，键 = 地区码(2bit) << 16 | 线区码 << 8 | 站码
 * - bus.tsv：巴士，键 = 巴士公司代码 << 16 | 站点码（站点为 0 表示只知道公司）
 */
object Stations {
    @Volatile
    private var rail: Map<Int, Station> = emptyMap()

    @Volatile
    private var busStops: Map<Long, String> = emptyMap()

    @Volatile
    private var busCompanies: Map<Int, String> = emptyMap()

    val loaded: Boolean get() = rail.isNotEmpty()

    /** 在后台线程调用，约几十毫秒。 */
    fun load(context: Context) {
        if (loaded) return
        val r = HashMap<Int, Station>(8192)
        context.assets.open("stations.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val p = line.split('\t')
                if (p.size >= 4) p[0].toIntOrNull(16)?.let { r[it] = Station(p[1], p[2], p[3]) }
            }
        }
        val stops = HashMap<Long, String>(8192)
        val companies = HashMap<Int, String>(256)
        context.assets.open("bus.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val p = line.split('\t')
                val key = p.getOrNull(0)?.toLongOrNull(16) ?: return@forEach
                if (p.size < 3) return@forEach
                val op = (key shr 16).toInt()
                if (p[2].isNotEmpty()) companies.putIfAbsent(op, p[2])
                if (p[1].isNotEmpty()) stops[key] = p[1]
            }
        }
        busStops = stops
        busCompanies = companies
        rail = r
    }

    fun find(area: Int, line: Int, station: Int): Station? =
        rail[((area and 0x03) shl 16) or ((line and 0xFF) shl 8) or (station and 0xFF)]

    fun busCompany(operator: Int): String? = busCompanies[operator and 0xFFFF]

    fun busStop(operator: Int, stop: Int): String? =
        if (stop == 0) null else busStops[((operator.toLong() and 0xFFFF) shl 16) or (stop.toLong() and 0xFFFF)]
}
