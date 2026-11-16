package com.example.melosegno

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.melosegno.data.LanguageSetting
import com.example.melosegno.data.NoteRepository
import com.example.melosegno.data.SettingsRepository
import com.example.melosegno.data.ThemeSetting
import com.example.melosegno.ui.MeLoSegnoApp
import com.example.melosegno.ui.theme.MeLoSegnoTheme
import com.example.melosegno.widget.PinnedNoteWidget
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * Id della nota da aprire, arrivato dal widget della nota fissata.
     * 0 significa "nessuna richiesta": si apre una nota nuova.
     */
    private var pendingOpenNoteId by mutableLongStateOf(0L)

    /**
     * La lingua salvata viene applicata alla base dell'intera Activity, cosi'
     * al'avvio (o al ritorno nell'app) qualunque componente — finestre, popup,
     * toast e l'activity stessa — nasce gia' nella lingua scelta.
     * Il cambio in corsa resta gestito da CompositionLocalProvider nel setContent.
     */
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(SettingsRepository.KEY_LANGUAGE, "system") ?: "system"
        val wrapped = if (language == "system") {
            newBase
        } else {
            val locale = when (language) {
                "it" -> Locale.ITALIAN
                else -> Locale.ENGLISH
            }
            val config = Configuration(newBase.resources.configuration)
            config.setLocales(LocaleList(locale))
            newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Richiesta di apertura dal widget: vale solo per l'avvio corrente.
        pendingOpenNoteId = intent?.getLongExtra(PinnedNoteWidget.EXTRA_NOTE_ID, 0L) ?: 0L
        setContent {
            val settingsRepo = remember { SettingsRepository(applicationContext) }
            val noteRepo = remember { NoteRepository(applicationContext) }
            var settings by remember { mutableStateOf(settingsRepo.load()) }

            // All'avvio l'icona nel drawer porta etichetta e colore salvati.
            LaunchedEffect(Unit) {
                settingsRepo.applyIconLabel(applicationContext, settings)
            }

            val darkTheme = when (settings.theme) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            MeLoSegnoTheme(darkTheme = darkTheme) {
                // La lingua scelta sostituisce quella del dispositivo: un contesto
                // con la configurazione aggiornata viene fornito a tutta l'app,
                // cosi' ogni stringa (menu, dialog, toast) viene tradotta.
                val appContext = applicationContext
                val localizedContext = remember(settings.language) {
                    if (settings.language == LanguageSetting.SYSTEM) {
                        appContext
                    } else {
                        val config = Configuration(appContext.resources.configuration)
                        config.setLocale(
                            when (settings.language) {
                                LanguageSetting.ITALIAN -> Locale.ITALIAN
                                else -> Locale.ENGLISH
                            }
                        )
                        appContext.createConfigurationContext(config)
                    }
                }
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration
                ) {
                    MeLoSegnoApp(
                        repository = noteRepo,
                        settingsRepo = settingsRepo,
                        settings = settings,
                        onSettingsChange = { settings = it },
                        openNoteRequest = pendingOpenNoteId,
                        onOpenNoteHandled = { pendingOpenNoteId = 0L }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // L'app era gia' aperta: si passa la richiesta alla UI corrente.
        pendingOpenNoteId = intent.getLongExtra(PinnedNoteWidget.EXTRA_NOTE_ID, 0L)
    }
}
