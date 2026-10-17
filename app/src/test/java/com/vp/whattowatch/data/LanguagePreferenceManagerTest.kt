package com.vp.whattowatch.data

import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LanguagePreferenceManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset preferences before each test
        context.getSharedPreferences(LanguagePreferenceManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(LanguagePreferenceManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testDefaultLanguageIsSystem() {
        val stored = LanguagePreferenceManager.getStoredLanguage(context)
        assertEquals(LanguagePreferenceManager.LANGUAGE_SYSTEM, stored)
    }

    @Test
    fun testSetLanguageEnglishPersists() {
        LanguagePreferenceManager.setLanguage(context, LanguagePreferenceManager.LANGUAGE_ENGLISH)
        val stored = LanguagePreferenceManager.getStoredLanguage(context)
        assertEquals("en", stored)
    }

    @Test
    fun testSetLanguageRussianPersists() {
        LanguagePreferenceManager.setLanguage(context, LanguagePreferenceManager.LANGUAGE_RUSSIAN)
        val stored = LanguagePreferenceManager.getStoredLanguage(context)
        assertEquals("ru", stored)
    }

    @Test
    fun testSetInvalidLanguageFallsBackToSystem() {
        LanguagePreferenceManager.setLanguage(context, "invalid_lang_code")
        val stored = LanguagePreferenceManager.getStoredLanguage(context)
        assertEquals(LanguagePreferenceManager.LANGUAGE_SYSTEM, stored)
    }

    @Test
    fun testApplyLocaleToContextSetsCorrectLocale() {
        LanguagePreferenceManager.setLanguage(context, "ru")
        val localizedContext = LanguagePreferenceManager.applyLocaleToContext(context)
        val locales = ConfigurationCompat.getLocales(localizedContext.resources.configuration)
        assertEquals("ru", locales[0]?.language)

        LanguagePreferenceManager.setLanguage(context, "en")
        val localizedContextEn = LanguagePreferenceManager.applyLocaleToContext(context)
        val localesEn = ConfigurationCompat.getLocales(localizedContextEn.resources.configuration)
        assertEquals("en", localesEn[0]?.language)
    }
}
