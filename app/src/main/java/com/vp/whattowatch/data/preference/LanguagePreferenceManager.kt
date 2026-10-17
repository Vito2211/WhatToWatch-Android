package com.vp.whattowatch.data.preference

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import com.vp.whattowatch.R
import java.util.Locale

data class LanguageOption(
    val tag: String,
    @param:StringRes val titleRes: Int
)

object LanguagePreferenceManager {
    const val PREFS_NAME = "what_to_watch_preferences"
    const val KEY_APP_LANGUAGE = "app_language"

    const val LANGUAGE_SYSTEM = ""
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_RUSSIAN = "ru"

    val supportedLanguages = listOf(
        LanguageOption(LANGUAGE_SYSTEM, R.string.language_system_default),
        LanguageOption(LANGUAGE_ENGLISH, R.string.language_english),
        LanguageOption(LANGUAGE_RUSSIAN, R.string.language_russian)
    )

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStoredLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }

    fun setLanguage(context: Context, languageTag: String) {
        val sanitizedTag = when (languageTag.lowercase()) {
            LANGUAGE_ENGLISH -> LANGUAGE_ENGLISH
            LANGUAGE_RUSSIAN -> LANGUAGE_RUSSIAN
            else -> LANGUAGE_SYSTEM
        }

        // Persist to SharedPreferences
        getPreferences(context).edit().putString(KEY_APP_LANGUAGE, sanitizedTag).apply()

        // Apply via Android 13+ (API 33+) LocaleManager if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val localeList = if (sanitizedTag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(sanitizedTag)
            }
            localeManager?.applicationLocales = localeList
        }
    }

    /**
     * Wraps the given context with the currently configured locale configuration.
     * Used by MainActivity.attachBaseContext to ensure all resources match the preferred language.
     */
    fun applyLocaleToContext(baseContext: Context): Context {
        val language = getStoredLanguage(baseContext)
        val targetLocale = if (language.isNotEmpty()) {
            Locale.forLanguageTag(language)
        } else {
            // System default via ConfigurationCompat
            val systemLocales = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
            if (!systemLocales.isEmpty) {
                systemLocales[0] ?: Locale.getDefault()
            } else {
                Locale.getDefault()
            }
        }

        Locale.setDefault(targetLocale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return baseContext.createConfigurationContext(config)
    }
}
