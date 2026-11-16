package com.example.melosegno.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.melosegno.R

/** Rosso del cuore del pulsante di sostegno. */
private val HeartRed = Color(0xFFD32F2F)

/**
 * Pagina Informazioni: nota su autore, versione e licenza,
 * con il pulsante per condividere il link all'app.
 */
@Composable
fun InfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Tasto indietro di sistema: torna all'editor invece di uscire dall'app.
    BackHandler { onBack() }

    // Sezioni a espansione in fondo alla pagina: licenze e privacy.
    var licensesExpanded by remember { mutableStateOf(false) }
    var apacheTextExpanded by remember { mutableStateOf(false) }
    var privacyExpanded by remember { mutableStateOf(false) }
    // Testo completo della licenza, caricato dalla risorsa raw.
    val apacheText = remember {
        runCatching {
            context.resources.openRawResource(R.raw.apache_license_2_0)
                .bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Su schermi larghi i contenuti restano alla larghezza leggibile, centrati.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back_desc)
                    )
                }
                Text(
                    text = stringResource(R.string.info_desc),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.version_label) + " " + versionName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        // Autore, con l'illustrazione dell'avatar in cornice rotonda.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.avatar_author),
                                contentDescription = stringResource(R.string.author_name),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.author_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.author_name),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Azioni: condivisione e sostegno con lo stesso stile di pillola.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    context.getString(R.string.share_message, context.getString(R.string.app_link))
                                )
                            }
                            // Il contesto localizzato non e' un contesto di
                            // Activity: il flag NEW_TASK va sull'intent del
                            // chooser, quello che viene avviato davvero.
                            val chooser = Intent.createChooser(shareIntent, null)
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.share_button),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Bolla unica di sostegno: pulsante in alto, testo sotto,
                    // entrambi dentro la bolla.
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.clickable {
                                    val donateIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(context.getString(R.string.donate_link))
                                    )
                                    // Vale come sopra: contesto non di Activity.
                                    donateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(donateIntent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_heart),
                                        contentDescription = null,
                                        tint = HeartRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.donate_button),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.donate_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Informativa sulla licenza, in fondo alla pagina.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_license),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.license_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Informativa sulla privacy: sezione cliccabile, prima delle
                // licenze perche' piu' importante per l'utente.
                SectionCard(
                    iconRes = R.drawable.ic_info,
                    titleRes = R.string.privacy_title,
                    expanded = privacyExpanded,
                    onToggle = { privacyExpanded = !privacyExpanded }
                ) {
                    Text(
                        text = stringResource(R.string.privacy_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        R.string.privacy_bullet_1,
                        R.string.privacy_bullet_2,
                        R.string.privacy_bullet_3,
                        R.string.privacy_bullet_4,
                        R.string.privacy_bullet_5,
                        R.string.privacy_bullet_6
                    ).forEach { bulletRes ->
                        Text(
                            text = "• " + stringResource(bulletRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Licenze dei componenti di terze parti: sezione cliccabile
                // con l'elenco dei componenti e il testo completo della licenza.
                SectionCard(
                    iconRes = R.drawable.ic_license,
                    titleRes = R.string.licenses_title,
                    expanded = licensesExpanded,
                    onToggle = { licensesExpanded = !licensesExpanded }
                ) {
                    Text(
                        text = stringResource(R.string.licenses_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LicenseEntry(
                        name = "Jetpack Compose e AndroidX",
                        author = "Google LLC / The Android Open Source Project",
                        projectUrl = "https://developer.android.com/jetpack/androidx"
                    )
                    LicenseEntry(
                        name = "Kotlin Standard Library, Coroutines e Annotations",
                        author = "JetBrains",
                        projectUrl = "https://github.com/JetBrains/kotlin"
                    )
                    LicenseEntry(
                        name = "JSpecify",
                        author = "Il progetto JSpecify",
                        projectUrl = "https://jspecify.org/"
                    )
                    LicenseEntry(
                        name = "Guava ListenableFuture",
                        author = "Google LLC",
                        projectUrl = "https://github.com/google/guava"
                    )
                    // Testo completo della licenza, a sua volta a espansione.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { apacheTextExpanded = !apacheTextExpanded }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(if (apacheTextExpanded) 90f else 270f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.license_full_text_title),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (apacheTextExpanded && apacheText.isNotBlank()) {
                        Text(
                            text = apacheText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * Apre un link nel browser: il contesto localizzato non e' un contesto di
 * Activity, quindi serve il flag NEW_TASK; se non c'e' un browser non lancia
 * eccezioni.
 */
private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

/** Voce dell'elenco licenze: nome, autore e link cliccabili. */
@Composable
private fun LicenseEntry(
    name: String,
    author: String,
    projectUrl: String
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "Apache License 2.0",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    openUrl(context, "https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            )
            Text(
                text = stringResource(R.string.license_project),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { openUrl(context, projectUrl) }
            )
        }
    }
}

/** Card a espansione usata per licenze e privacy, nello stile delle altre. */
@Composable
private fun SectionCard(
    iconRes: Int,
    titleRes: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(titleRes),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(if (expanded) 90f else 270f)
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
