package com.gadagojuanilkumar.fmradioindiaak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// false = deep blue background with sky-blue accents
// true  = light sky-blue background (also set themes.xml parent to
//         android:Theme.Material.Light.NoActionBar)
private const val LIGHT_SKY = true

private val DarkSkyColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00263A),
    primaryContainer = Color(0xFF14507A),
    onPrimaryContainer = Color(0xFFE3F4FF),
    secondary = Color(0xFF7CC4FF),
    onSecondary = Color(0xFF00263A),
    secondaryContainer = Color(0xFF1B3B5C),
    onSecondaryContainer = Color(0xFF9BDAFF),
    background = Color(0xFF0B1A2B),
    onBackground = Color(0xFFE8F3FF),
    surface = Color(0xFF0B1A2B),
    onSurface = Color(0xFFE8F3FF),
    surfaceVariant = Color(0xFF1A2F47),
    onSurfaceVariant = Color(0xFFA9C4DD),
    outline = Color(0xFF3C5C7C)
)

private val LightSkyColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8FCDF5),
    onPrimaryContainer = Color(0xFF062B4A),
    secondary = Color(0xFF2A7DB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5DDF7),
    onSecondaryContainer = Color(0xFF0B3C66),
    background = Color(0xFFD9EEFB),
    onBackground = Color(0xFF0B2540),
    surface = Color(0xFFEAF6FE),
    onSurface = Color(0xFF0B2540),
    surfaceVariant = Color(0xFFC3E0F5),
    onSurfaceVariant = Color(0xFF35566F),
    outline = Color(0xFF6C93B5)
)

@Composable
fun FMRadioIndiaAKTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (LIGHT_SKY) LightSkyColors else DarkSkyColors,
        typography = Typography,
        content = content
    )
}