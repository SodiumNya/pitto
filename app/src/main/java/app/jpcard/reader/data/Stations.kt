package app.jpcard.reader.data

import android.content.Context

data class Station(val name: String, val company: String, val line: String)

/**
 * 车站代码表（assets/stations.tsv，由 tools/gen_stations.py 生成）。
 * 键 = 地区码(2bit) << 16 | 线区码 << 8 | 站码。
 */
object Stations {
    @Volatile
    private var map: Map<Int, Station> = emptyMap()

    val loaded: Boolean get() = map.isNotEmpty()

    /** 在后台线程调用，约几十毫秒。 */
    fun load(context: Context) {
        if (loaded) return
        val m = HashMap<Int, Station>(8192)
        context.assets.open("stations.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val p = line.split('\t')
                if (p.size >= 4) {
                    p[0].toIntOrNull(16)?.let { m[it] = Station(p[1], p[2], p[3]) }
                }
            }
        }
        map = m
    }

    fun find(area: Int, line: Int, station: Int): Station? =
        map[((area and 0x03) shl 16) or ((line and 0xFF) shl 8) or (station and 0xFF)]
}
