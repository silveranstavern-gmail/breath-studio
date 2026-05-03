package com.ponderingsilver.breathstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TealGlow,
    onPrimary = Linen,
    secondary = WarmSand,
    onSecondary = DeepOcean,
    tertiary = SoftSage,
    onTertiary = Linen,
    background = Mist,
    onBackground = DeepPine,
    surface = Linen,
    onSurface = DeepPine,
    surfaceVariant = Color(0xFFE7E0D3),
    onSurfaceVariant = Color(0xFF4F5F60),
    outline = Color(0xFF98ABA7),
)

private val DarkColorScheme = darkColorScheme(
    primary = SeaMist,
    onPrimary = DeepOcean,
    secondary = TwilightSand,
    onSecondary = DeepOcean,
    tertiary = Cloud,
    onTertiary = DeepOcean,
    background = DeepOcean,
    onBackground = Linen,
    surface = Color(0xFF112427),
    onSurface = Linen,
    surfaceVariant = Color(0xFF213639),
    onSurfaceVariant = Color(0xFFC7D7D4),
    outline = Color(0xFF5E7772),
)

@Composable
fun BreathStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
