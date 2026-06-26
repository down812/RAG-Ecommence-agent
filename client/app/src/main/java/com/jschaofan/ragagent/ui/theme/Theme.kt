package com.jschaofan.ragagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    secondary = EvidenceGreen,
    tertiary = CommerceRed,
    background = DarkBackground,
    onBackground = AppSurface,
    surface = DarkSurface,
    onSurface = AppSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = AppOutline,
    outline = AppOutline,
    outlineVariant = DarkOutlineVariant,
    error = CommerceRed,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = AppSurface,
    secondary = EvidenceGreen,
    onSecondary = AppSurface,
    tertiary = CommerceRed,
    onTertiary = AppSurface,
    primaryContainer = SoftBlue,
    onPrimaryContainer = BrandBlue,
    secondaryContainer = SoftBlue,
    onSecondaryContainer = BrandBlue,
    tertiaryContainer = SoftRose,
    onTertiaryContainer = OnSoftRose,
    background = AppBackground,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppOnSurfaceVariant,
    outline = AppOutline,
    outlineVariant = AppOutlineVariant,
    error = CommerceRed,
    errorContainer = SoftRose,
    onErrorContainer = OnSoftRose,
)

@Composable
fun RAGGuideAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
