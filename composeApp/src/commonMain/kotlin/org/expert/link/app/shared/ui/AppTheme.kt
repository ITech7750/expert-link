package org.expert.link.app.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = Color(0xFF2057A6),
    onPrimary = Color.White,
    secondary = Color(0xFF3D6B35),
    onSecondary = Color.White,
    tertiary = Color(0xFF785A00),
    background = Color(0xFFF7F8FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8ECF4),
    outline = Color(0xFF6F7785),
    error = Color(0xFFB3261E),
)

/** Тема клиентского приложения. */
@Composable
fun ExpertLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content,
    )
}
