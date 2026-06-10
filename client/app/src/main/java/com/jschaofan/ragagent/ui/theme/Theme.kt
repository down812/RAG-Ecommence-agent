package com.jschaofan.ragagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    secondary = SoftBlue,
    tertiary = WarmAccent,
    background = DarkBackground,
    surface = DarkSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandBlue,
    tertiary = WarmAccent,
    primaryContainer = SoftBlue,
    background = AppBackground,
    surface = AppSurface,
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
