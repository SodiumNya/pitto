package app.jpcard.reader.nfc

import android.content.Context
import app.jpcard.reader.R

/**
 * 交通系 IC 卡种类。
 *
 * 各发卡方都会在 CJRC 系统（0x0003）里登记几个自己独有的服务码，
 * 用 Request Service 命令查询这些服务码是否存在即可分辨卡种。
 * 服务码清单参考 Metrodroid 项目的整理（SuicaUtil.kt）。
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

        /** 所有探测用服务码，顺序与 [detectable] 展开后一致（共 20 个，一条命令即可查完）。 */
        val allProbes: IntArray = detectable.flatMap { it.probes.asList() }.toIntArray()

        /** 根据每个服务码是否存在判断卡种；命中且只命中一种时才认定。 */
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

    /** 显示名：卡名是专有名词不翻译，只有“未知卡种”按语言显示。 */
    fun displayName(context: Context): String =
        if (this == UNKNOWN) context.getString(R.string.card_type_unknown) else label
}
