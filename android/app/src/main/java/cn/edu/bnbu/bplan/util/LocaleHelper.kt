package cn.edu.bnbu.bplan.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import cn.edu.bnbu.bplan.R
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val locale: Locale,
    @StringRes val labelRes: Int
) {
    SIMPLIFIED_CHINESE("zh-CN", Locale.SIMPLIFIED_CHINESE, R.string.lang_zh_cn),
    TRADITIONAL_CHINESE_HK("zh-HK", Locale("zh", "HK"), R.string.lang_zh_hk),
    ENGLISH("en", Locale.ENGLISH, R.string.lang_en);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when (code) {
                TRADITIONAL_CHINESE_HK.code -> TRADITIONAL_CHINESE_HK
                ENGLISH.code -> ENGLISH
                else -> SIMPLIFIED_CHINESE
            }
        }
    }
}

object LocaleHelper {
    private const val PREFS_NAME = "bplan_locale_prefs"
    private const val KEY_LANGUAGE = "selected_app_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.SIMPLIFIED_CHINESE.code)
        return AppLanguage.fromCode(code)
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun wrap(context: Context, language: AppLanguage = getSavedLanguage(context)): Context {
        val locale = language.locale
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    fun getLocalizedConfiguration(context: Context, language: AppLanguage): Configuration {
        val locale = language.locale
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return config
    }
}
