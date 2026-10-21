package dev.klang.player.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleLight = Color(0xFF5A43B5)
private val PurpleDark = Color(0xFFB9A6FF)

private fun accentColor(name: String, dark: Boolean): Color = when (name) {
    "blue" -> if (dark) Color(0xFFAAC7FF) else Color(0xFF365FA0)
    "green" -> if (dark) Color(0xFF9DDBB0) else Color(0xFF216B48)
    "orange" -> if (dark) Color(0xFFFFB870) else Color(0xFF8B5000)
    else -> if (dark) PurpleDark else PurpleLight
}

@Composable
fun KlangTheme(mode: String = "system", accent: String = "purple", content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> systemDark
    }
    val primary = accentColor(accent, dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF21164A),
            secondary = when (accent) {
                "blue" -> Color(0xFFB7C8FF)
                "green" -> Color(0xFFB5DCC0)
                "orange" -> Color(0xFFFFD0A0)
                else -> Color(0xFFB6C8FF)
            },
            tertiary = Color(0xFF9BDCCB),
            background = if (mode == "amoled") Color.Black else Color(0xFF101116),
            surface = if (mode == "amoled") Color.Black else Color(0xFF181A21),
            surfaceVariant = if (mode == "amoled") Color(0xFF101010) else Color(0xFF262832),
            onSurface = Color(0xFFE7E5EE),
            onSurfaceVariant = Color(0xFFB8B6C4),
            outline = Color(0xFF454653)
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = Color(0xFF4D5E9B),
            tertiary = Color(0xFF226B5D),
            background = Color(0xFFF8F7FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEDEAF5)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
