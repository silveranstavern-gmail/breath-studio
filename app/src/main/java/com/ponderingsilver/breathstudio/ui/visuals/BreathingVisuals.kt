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
        val ringColor = lerp(
            start = stepColor.copy(alpha = 0.28f),
            stop = stepColor.copy(alpha = 0.68f),
            fraction = stageProgress.coerceIn(0f, 1f),
        )

        drawCircle(
            color = stepColor.copy(alpha = 0.08f),
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
