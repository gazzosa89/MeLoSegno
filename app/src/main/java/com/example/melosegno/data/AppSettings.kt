package com.example.melosegno.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Modalita' di scrittura scelta dall'utente. */
enum class WritingMode { TEXT, HAND }

/** Scelta del tema dell'app. */
enum class ThemeSetting { LIGHT, DARK, SYSTEM }

/** Lingua dell'app. SYSTEM segue la lingua del dispositivo. */
enum class LanguageSetting { SYSTEM, ITALIAN, ENGLISH }

/** Impostazioni dell'app. */
data class AppSettings(
    // Modalita' preselezionata all'apertura e per ogni nota nuova
    val defaultMode: WritingMode = WritingMode.TEXT,
    // Etichetta nel drawer: false = "Note", true = "MeLoSegno"
    val useShortLabel: Boolean = false,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val language: LanguageSetting = LanguageSetting.SYSTEM
)

/**
 * Persistenza delle impostazioni in SharedPreferences.
 * L'etichetta del launcher si cambia attivando uno dei due activity-alias.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        defaultMode = when (prefs.getString(KEY_MODE, "text")) {
            "hand" -> WritingMode.HAND
            else -> WritingMode.TEXT
        },
        useShortLabel = prefs.getBoolean(KEY_SHORT_LABEL, false),
        theme = when (prefs.getString(KEY_THEME, "system")) {
            "light" -> ThemeSetting.LIGHT
            "dark" -> ThemeSetting.DARK
            else -> ThemeSetting.SYSTEM
        },
        language = when (prefs.getString(KEY_LANGUAGE, "system")) {
            "it" -> LanguageSetting.ITALIAN
            "en" -> LanguageSetting.ENGLISH
            else -> LanguageSetting.SYSTEM
        }
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_MODE, if (settings.defaultMode == WritingMode.HAND) "hand" else "text")
            .putBoolean(KEY_SHORT_LABEL, settings.useShortLabel)
            .putString(
                KEY_THEME,
                when (settings.theme) {
                    ThemeSetting.LIGHT -> "light"
                    ThemeSetting.DARK -> "dark"
                    else -> "system"
                }
            )
            .putString(
                KEY_LANGUAGE,
                when (settings.language) {
                    LanguageSetting.ITALIAN -> "it"
                    LanguageSetting.ENGLISH -> "en"
                    else -> "system"
                }
            )
            // Commit sincrono: il riavvio forzato del cambio lingua non deve
            // mai trovarsi le preferenze ancora non scritte su disco.
            .commit()
    }

    /** Attiva l'alias del launcher che porta l'etichetta scelta. */
    fun applyIconLabel(context: Context, useShortLabel: Boolean) {
        setAliasEnabled(context, ALIAS_NOTE, enabled = !useShortLabel)
        setAliasEnabled(context, ALIAS_FULL, enabled = useShortLabel)
    }

    private fun setAliasEnabled(context: Context, alias: String, enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, alias),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    companion object {
        // Pubbliche perche' usate anche da MainActivity in attachBaseContext.
        const val PREFS_NAME = "MeLoSegno_settings"
        const val KEY_LANGUAGE = "language"
        private const val KEY_MODE = "default_mode"
        private const val KEY_SHORT_LABEL = "short_label"
        private const val KEY_THEME = "theme"
        private const val ALIAS_NOTE = "com.example.melosegno.LauncherNote"
        private const val ALIAS_FULL = "com.example.melosegno.LauncherMeLoSegno"
    }
}
