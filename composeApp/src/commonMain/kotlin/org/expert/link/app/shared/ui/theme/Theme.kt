package org.expert.link.app.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EnterpriseLightScheme = lightColorScheme(
    primary = Color(0xFF156F67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F3EE),
    onPrimaryContainer = Color(0xFF0E3C38),
    secondary = Color(0xFF4C6160),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E6E3),
    onSecondaryContainer = Color(0xFF223332),
    tertiary = Color(0xFF9A6B17),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF412700),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFFE2DE),
    onErrorContainer = Color(0xFF601410),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1D1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D1E),
    surfaceVariant = Color(0xFFE9EEF0),
    onSurfaceVariant = Color(0xFF5E6A6E),
    outline = Color(0xFFC6D0D3),
    outlineVariant = Color(0xFFDDE4E6),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEDEFF1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFEFE),
    surfaceContainer = Color(0xFFF7F9FA),
    surfaceContainerHigh = Color(0xFFF1F4F5),
    surfaceContainerHighest = Color(0xFFEAF0F1),
)

private val EnterpriseDarkScheme = darkColorScheme(
    primary = Color(0xFF84D4C8),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF0F534D),
    onPrimaryContainer = Color(0xFFD7F3EE),
    secondary = Color(0xFFB4CAC7),
    onSecondary = Color(0xFF1E3130),
    secondaryContainer = Color(0xFF354847),
    onSecondaryContainer = Color(0xFFD0E6E3),
    tertiary = Color(0xFFF6BD63),
    onTertiary = Color(0xFF4A2E00),
    tertiaryContainer = Color(0xFF6A4700),
    onTertiaryContainer = Color(0xFFFFE0B2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690E07),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1415),
    onBackground = Color(0xFFE8ECEC),
    surface = Color(0xFF111718),
    onSurface = Color(0xFFE8ECEC),
    surfaceVariant = Color(0xFF384447),
    onSurfaceVariant = Color(0xFFBAC7CA),
    outline = Color(0xFF859296),
    outlineVariant = Color(0xFF384447),
    surfaceBright = Color(0xFF353B3C),
    surfaceDim = Color(0xFF111718),
    surfaceContainerLowest = Color(0xFF0A0F10),
    surfaceContainerLow = Color(0xFF181E1F),
    surfaceContainer = Color(0xFF1C2223),
    surfaceContainerHigh = Color(0xFF262C2D),
    surfaceContainerHighest = Color(0xFF303637),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val ignoredDynamicColor = dynamicColor
    MaterialTheme(
        colorScheme = if (darkTheme) EnterpriseDarkScheme else EnterpriseLightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
