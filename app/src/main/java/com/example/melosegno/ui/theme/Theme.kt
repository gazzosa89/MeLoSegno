package com.example.melosegno.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = InkBlue,
    onPrimary = OnInkBlue,
    primaryContainer = InkBlueContainer,
    onPrimaryContainer = OnInkBlueContainer,
    secondary = Slate,
    onSecondary = OnSlate,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = OnSlateContainer,
    tertiary = Plum,
    onTertiary = OnPlum,
    tertiaryContainer = PlumContainer,
    onTertiaryContainer = OnPlumContainer,
    background = PaperLight,
    onBackground = OnPaperLight,
    surface = PaperLight,
    onSurface = OnPaperLight,
    surfaceVariant = PaperVariantLight,
    onSurfaceVariant = OnPaperVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColors = darkColorScheme(
    primary = InkBlueDark,
    onPrimary = OnInkBlueDark,
    primaryContainer = InkBlueContainerDark,
    onPrimaryContainer = OnInkBlueContainerDark,
    secondary = SlateDark,
    onSecondary = OnSlateDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = OnSlateContainerDark,
    tertiary = PlumDark,
    onTertiary = OnPlumDark,
    tertiaryContainer = PlumContainerDark,
    onTertiaryContainer = OnPlumContainerDark,
    background = PaperDark,
    onBackground = OnPaperDark,
    surface = PaperDark,
    onSurface = OnPaperDark,
    surfaceVariant = PaperVariantDark,
    onSurfaceVariant = OnPaperVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

/**
 * Tema dell'app. Il tema chiaro/scuro arriva dalle Impostazioni;
 * su Android 12+ si usa il colore dinamico del sistema, altrimenti
 * la palette "inchiostro e carta".
 */
@Composable
fun MeLoSegnoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color e' disponibile su Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
