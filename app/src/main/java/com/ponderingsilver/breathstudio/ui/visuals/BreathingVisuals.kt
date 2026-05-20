package com.ponderingsilver.breathstudio.ui.visuals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.ponderingsilver.breathstudio.ui.theme.isLight

@Composable
fun BreathingVisual(
    stageProgress: Float,
    stepColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = center
        val radius = size.minDimension * 0.52f
        val ringInset = size.minDimension * 0.13f
        val ringBounds = Size(size.width - (ringInset * 2f), size.height - (ringInset * 2f))
        
        val isLightColor = stepColor.isLight()
        val baseAlpha = if (isLightColor) 0.22f else 0.38f
        val targetAlpha = if (isLightColor) 0.62f else 0.82f

        val ringColor = lerp(
            start = stepColor.copy(alpha = baseAlpha),
            stop = stepColor.copy(alpha = targetAlpha),
            fraction = stageProgress.coerceIn(0f, 1f),
        )

        drawCircle(
            color = stepColor.copy(alpha = if (isLightColor) 0.06f else 0.12f),
            center = center,
            radius = radius,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.09f),
            radius = size.minDimension * 0.38f,
            style = Stroke(width = size.minDimension * 0.012f),
        )

        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * stageProgress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = size.minDimension * 0.028f, cap = StrokeCap.Round),
            topLeft = Offset(ringInset, ringInset),
            size = ringBounds,
        )
    }
}
