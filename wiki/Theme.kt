package org.example.project.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

val md_theme_light_primary = Color(0xFF00488d)
val md_theme_light_onPrimary = Color(0xFFffffff)
val md_theme_light_primaryContainer = Color(0xFF005fb8)
val md_theme_light_onPrimaryContainer = Color(0xFFcadcff)
val md_theme_light_secondary = Color(0xFF4a6077)
val md_theme_light_onSecondary = Color(0xFFffffff)
val md_theme_light_secondaryContainer = Color(0xFFcbe2fc)
val md_theme_light_onSecondaryContainer = Color(0xFF4f657b)
val md_theme_light_tertiary = Color(0xFF960010)
val md_theme_light_onTertiary = Color(0xFFffffff)
val md_theme_light_tertiaryContainer = Color(0xFFbc1c21)
val md_theme_light_onTertiaryContainer = Color(0xFFffd0cb)
val md_theme_light_error = Color(0xFFba1a1a)
val md_theme_light_errorContainer = Color(0xFFffdad6)
val md_theme_light_onError = Color(0xFFffffff)
val md_theme_light_onErrorContainer = Color(0xFF93000a)
val md_theme_light_background = Color(0xFFf7f9fc)
val md_theme_light_onBackground = Color(0xFF191c1e)
val md_theme_light_surface = Color(0xFFf7f9fc)
val md_theme_light_onSurface = Color(0xFF191c1e)
val md_theme_light_surfaceVariant = Color(0xFFe0e3e6)
val md_theme_light_onSurfaceVariant = Color(0xFF424752)
val md_theme_light_outline = Color(0xFF727783)
val md_theme_light_inverseOnSurface = Color(0xFFeff1f4)
val md_theme_light_inverseSurface = Color(0xFF2d3133)
val md_theme_light_inversePrimary = Color(0xFFa8c8ff)

val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
