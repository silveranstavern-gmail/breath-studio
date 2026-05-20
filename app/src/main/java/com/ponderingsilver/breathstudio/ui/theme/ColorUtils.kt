package com.ponderingsilver.breathstudio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Determines if a color is "light" based on its relative luminance.
 * Uses the WCAG threshold of 0.5.
 */
fun Color.isLight(): Boolean = this.luminance() > 0.5f

/**
 * Returns a contrasting color (either [onLight] or [onDark]) based on the luminance of this color.
 */
fun Color.contrastColor(
    onLight: Color = Color(0xFF18353A), // DeepPine
    onDark: Color = Color(0xFFF9F6F0)   // Linen
): Color = if (this.isLight()) onLight else onDark

/**
 * Adjusts the color for visibility against a specific background.
 * If the contrast is too low, it returns a modified version of the color or a fallback.
 */
fun Color.ensureVisible(
    background: Color,
    minContrastThreshold: Float = 0.15f
): Color {
    val lum1 = this.luminance()
    val lum2 = background.luminance()
    val diff = kotlin.math.abs(lum1 - lum2)
    
    return if (diff < minContrastThreshold) {
        if (lum2 > 0.5f) {
            // Background is light, darken this color
            this.copy(
                red = (this.red * 0.7f),
                green = (this.green * 0.7f),
                blue = (this.blue * 0.7f)
            )
        } else {
            // Background is dark, lighten this color
            this.copy(
                red = (this.red + (1f - this.red) * 0.3f),
                green = (this.green + (1f - this.green) * 0.3f),
                blue = (this.blue + (1f - this.blue) * 0.3f)
            )
        }
    } else {
        this
    }
}
