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

/** Una variante di colore dello sfondo dell'icona del launcher. */
data class IconColorOption(val key: String, val argb: Long)

/** Impostazioni dell'app. */
data class AppSettings(
    // Modalita' preselezionata all'apertura e per ogni nota nuova
    val defaultMode: WritingMode = WritingMode.TEXT,
    // Etichetta nel drawer: false = "Note", true = "MeLoSegno"
    val useShortLabel: Boolean = false,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val language: LanguageSetting = LanguageSetting.SYSTEM,
    // Sfondo dell'icona: chiave di [ICON_COLOR_OPTIONS], "default" = blu del logo
    val iconColorKey: String = "default"
)

/**
 * Persistenza delle impostazioni in SharedPreferences.
 * Etichetta e colore dell'icona del launcher si cambiano attivando uno solo
 * degli activity-alias: per ogni combinazione esiste un alias nel manifest.
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
        },
        iconColorKey = prefs.getString(KEY_ICON_COLOR, "default")?.let { key ->
            if (ICON_COLOR_OPTIONS.any { it.key == key }) key else "default"
        } ?: "default"
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
            .putString(KEY_ICON_COLOR, settings.iconColorKey)
            // Commit sincrono: il riavvio forzato del cambio lingua non deve
            // mai trovarsi le preferenze ancora non scritte su disco.
            .commit()
    }

    /**
     * Attiva l'alias del launcher che porta l'etichetta e il colore scelti:
     * tutti gli altri alias vengono disabilitati.
     */
    fun applyIconLabel(context: Context, settings: AppSettings) {
        val target = aliasName(settings.useShortLabel, settings.iconColorKey)
        ALL_ALIASES.forEach { alias ->
            setAliasEnabled(context, alias, enabled = alias == target)
        }
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
        private const val KEY_ICON_COLOR = "icon_color"

        // Chiavi dei colori: devono corrispondere ai nomi delle risorse
        // ic_launcher_bg_* e agli alias dichiarati nel manifest.
        val ICON_COLOR_OPTIONS = listOf(
            IconColorOption("default", 0xFF4584C8), // blu del logo
            IconColorOption("1c1b1f", 0xFF1C1B1F), // nero
            IconColorOption("616161", 0xFF616161), // grigio
            IconColorOption("1565c0", 0xFF1565C0), // blu
            IconColorOption("00838f", 0xFF00838F), // petrolio
            IconColorOption("2e7d32", 0xFF2E7D32), // verde
            IconColorOption("c62828", 0xFFC62828), // rosso
            IconColorOption("ef6c00", 0xFFEF6C00), // arancione
            IconColorOption("f9a825", 0xFFF9A825), // giallo
            IconColorOption("6a1b9a", 0xFF6A1B9A), // viola
            IconColorOption("ad1457", 0xFFAD1457), // rosa
            IconColorOption("ffffff", 0xFFFFFFFF)  // bianco
        )

        // Suffissi di etichetta usati nei nomi degli alias del manifest.
        private const val PKG = "com.example.melosegno"
        private const val LABEL_NOTE = "Note"
        private const val LABEL_FULL = "Full"

        fun aliasName(useShortLabel: Boolean, colorKey: String): String {
            val label = if (useShortLabel) LABEL_FULL else LABEL_NOTE
            return "$PKG.Launcher_${label}_$colorKey"
        }

        // Tutti gli alias possibili, per disabilitare quelli non attivi.
        val ALL_ALIASES: List<String> = run {
            val keys = ICON_COLOR_OPTIONS.map { it.key }
            listOf(LABEL_NOTE, LABEL_FULL).flatMap { label ->
                keys.map { key -> "$PKG.Launcher_${label}_$key" }
            }
        }
    }
}
