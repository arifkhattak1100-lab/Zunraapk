package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZorniaColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = BackgroundObsidian,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = CyanNeonLight,
    secondary = VioletElectric,
    onSecondary = BackgroundObsidian,
    secondaryContainer = VioletDeep,
    onSecondaryContainer = VioletGlow,
    tertiary = RoseNeon,
    onTertiary = BackgroundObsidian,
    background = BackgroundObsidian,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RoseWarm,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZorniaColorScheme,
        typography = Typography,
        content = content
    )
}
