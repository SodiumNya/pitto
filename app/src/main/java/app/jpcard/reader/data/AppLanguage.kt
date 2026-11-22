package app.jpcard.reader.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import app.jpcard.reader.R
import java.util.Locale

/**
 * App 内的语言选择。空字符串表示“跟随系统”。
 * - Android 13+：使用系统的“应用语言”接口，与系统设置里的 Pitto 语言是同一个设置。
 * - Android 8~12：自己保存选择，在 Activity 创建时套用。
 */
object AppLanguage {
    /** 可选语言（BCP-47）。顺序即设置页里的显示顺序。 */
    val options = listOf("", "en", "zh-Hans", "zh-Hant", "ja", "ko")

    /** 各语言用自己的写法显示，无论当前界面是什么语言都能认出来。 */
    private val names = mapOf(
        "en" to "English",
        "zh-Hans" to "简体中文",
        "zh-Hant" to "繁體中文",
        "ja" to "日本語",
        "ko" to "한국어",
    )

    private const val PREFS = "settings"
    private const val KEY = "app_language"

    fun label(context: Context, tag: String): String =
        names[tag] ?: context.getString(R.string.language_system)

    /** 当前选择（规整成 [options] 里的值）。 */
    fun current(context: Context): String {
        val raw = if (Build.VERSION.SDK_INT >= 33) {
            val list = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (list.isEmpty) "" else list[0].toLanguageTag()
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        }
        return normalize(raw)
    }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // 系统会自动重建界面
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /** Android 8~12：在 attachBaseContext 里套用保存的语言。 */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = base.resources.configuration.apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    /** 把系统给的语言标签（如 zh-Hant-TW、ja-JP）对应到 [options]。 */
    private fun normalize(tag: String): String {
        if (tag.isEmpty()) return ""
        val locale = Locale.forLanguageTag(tag)
        return when (locale.language) {
            "zh" -> if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) "zh-Hant" else "zh-Hans"
            "en", "ja", "ko" -> locale.language
            else -> ""
        }
    }
}
