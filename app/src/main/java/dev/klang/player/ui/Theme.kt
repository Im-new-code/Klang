package dev.klang.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = darkColorScheme(
    primary = Color(0xFFB9A6FF), onPrimary = Color(0xFF25115C),
    secondary = Color(0xFFB6C8FF), tertiary = Color(0xFF9BDCCB),
    background = Color(0xFF101116), surface = Color(0xFF181A21),
    surfaceVariant = Color(0xFF262832), onSurface = Color(0xFFE7E5EE),
    onSurfaceVariant = Color(0xFFB8B6C4), outline = Color(0xFF454653)
)
private val Day = lightColorScheme(
    primary = Color(0xFF5A43B5), secondary = Color(0xFF4D5E9B),
    tertiary = Color(0xFF226B5D), background = Color(0xFFF8F7FC),
    surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFEDEAF5)
)
@Composable
fun KlangTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Night else Day, content = content)
}
