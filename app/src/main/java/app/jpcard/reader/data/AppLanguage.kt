package app.jpcard.reader.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import app.jpcard.reader.R
import java.util.Locale

/**
 * In-app language choice. An empty tag means "follow the system".
 * - Android 13+: uses the system per-app language API, i.e. the same setting as Pitto's language in system settings.
 * - Android 8-12: the choice is stored by the app and applied when the activity is created.
 */
object AppLanguage {
    /** Selectable languages (BCP-47), in the order shown in settings. */
    val options = listOf("", "en", "zh-Hans", "zh-Hant", "ja", "ko")

    /** Each language is shown in its own script, so it is recognizable whatever the current UI language is. */
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

    /** The current choice, normalized to one of [options]. */
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
            // The system re-creates the activity by itself.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /** Android 8-12: applies the stored language in attachBaseContext. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = base.resources.configuration.apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    /** Maps a system locale tag (e.g. zh-Hant-TW, ja-JP) to one of [options]. */
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
