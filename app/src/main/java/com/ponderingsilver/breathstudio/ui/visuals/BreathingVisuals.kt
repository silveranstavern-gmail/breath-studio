package com.ponderingsilver.breathstudio.ui.visuals

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import kotlin.math.min

@Composable
fun BreathingVisual(
    visualMode: BreathingVisualMode,
    action: BreathAction,
    stepProgress: Float,
    cycleProgress: Float,
    sessionProgress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (visualMode) {
            BreathingVisualMode.Circle -> CircleBreathingVisual(
                action = action,
                stepProgress = stepProgress,
                sessionProgress = sessionProgress,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
            BreathingVisualMode.SquareTracer -> SquareTracerVisual(
                action = action,
                stepProgress = stepProgress,
                cycleProgress = cycleProgress,
                sessionProgress = sessionProgress,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CircleBreathingVisual(
    action: BreathAction,
    stepProgress: Float,
    sessionProgress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val targetScale = remember(action, stepProgress) {
        when (action) {
            BreathAction.Inhale -> 0.72f + (0.42f * stepProgress)
            BreathAction.Exhale -> 1.14f - (0.42f * stepProgress)
            BreathAction.HoldIn -> 1.12f
            BreathAction.HoldOut -> 0.72f
            BreathAction.Rest -> 0.84f
        }
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "circle-scale",
    )
    val ambientTransition = rememberInfiniteTransition(label = "circle-ambient")
    val auraPulse by ambientTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 2_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura-pulse",
    )

    val palette = paletteForAction(action, accent)

    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.28f * animatedScale
        val center = center

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.glow.copy(alpha = 0.60f),
                    Color.Transparent,
                ),
                radius = radius * 2.55f * auraPulse,
                center = center,
            ),
            radius = radius * 2.55f * auraPulse,
            center = center,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = size.minDimension * 0.39f,
            style = Stroke(width = size.minDimension * 0.015f),
        )

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    palette.ring.copy(alpha = 0.18f),
                    palette.ring,
                    palette.edge.copy(alpha = 0.30f),
                    palette.ring.copy(alpha = 0.18f),
                ),
            ),
            startAngle = -90f,
            sweepAngle = 360f * sessionProgress,
            useCenter = false,
            style = Stroke(width = size.minDimension * 0.032f, cap = StrokeCap.Round),
            topLeft = Offset(size.width * 0.11f, size.height * 0.11f),
            size = Size(size.width * 0.78f, size.height * 0.78f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.core,
                    palette.edge,
                ),
                radius = radius,
                center = center,
            ),
            radius = radius,
            center = center,
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = radius * 0.46f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
            blendMode = BlendMode.Screen,
        )
    }
}

@Composable
private fun SquareTracerVisual(
    action: BreathAction,
    stepProgress: Float,
    cycleProgress: Float,
    sessionProgress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val ambientTransition = rememberInfiniteTransition(label = "square-ambient")
    val glowPulse by ambientTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "square-glow",
    )
    val palette = paletteForAction(action, accent)
    val activeProgress = when (action) {
        BreathAction.HoldIn, BreathAction.HoldOut -> cycleProgress
        else -> cycleProgress
    }

    Canvas(modifier = modifier) {
        val side = min(size.width, size.height) * 0.62f
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val strokeWidth = side * 0.045f
        val innerGlowRadius = side * (0.10f + (0.03f * stepProgress))

        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = topLeft,
            size = Size(side, side),
            cornerRadius = CornerRadius(side * 0.12f),
            style = Stroke(width = strokeWidth),
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.ring.copy(alpha = 0.26f),
                    palette.edge.copy(alpha = 0.85f),
                ),
                start = topLeft,
                end = Offset(topLeft.x + side, topLeft.y + side),
            ),
            topLeft = topLeft,
            size = Size(side, side),
            cornerRadius = CornerRadius(side * 0.12f),
            style = Stroke(width = strokeWidth * 0.42f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.glow.copy(alpha = 0.48f),
                    Color.Transparent,
                ),
                radius = side * 0.44f * glowPulse,
                center = center,
            ),
            radius = side * 0.44f * glowPulse,
            center = center,
        )

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    palette.ring.copy(alpha = 0.14f),
                    palette.ring,
                    palette.edge.copy(alpha = 0.24f),
                    palette.ring.copy(alpha = 0.14f),
                ),
            ),
            startAngle = -90f,
            sweepAngle = 360f * sessionProgress,
            useCenter = false,
            style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Round),
            topLeft = Offset(size.width * 0.10f, size.height * 0.10f),
            size = Size(size.width * 0.80f, size.height * 0.80f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.core.copy(alpha = 0.95f),
                    palette.glow.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                radius = innerGlowRadius * 2.6f,
                center = center,
            ),
            radius = innerGlowRadius * 2.6f,
            center = center,
        )
    }
}

private data class ActionPalette(
    val core: Color,
    val edge: Color,
    val glow: Color,
    val ring: Color,
)

private fun paletteForAction(action: BreathAction, accent: Color): ActionPalette = when (action) {
    BreathAction.Inhale -> ActionPalette(
        core = Color(0xFFD8FFF2),
        edge = accent,
        glow = Color(0xFF74DACA),
        ring = Color(0xFF95F1E0),
    )
    BreathAction.HoldIn -> ActionPalette(
        core = Color(0xFFFDE9B9),
        edge = Color(0xFFE5BC70),
        glow = Color(0xFFF1D28B),
        ring = Color(0xFFF7D89A),
    )
    BreathAction.Exhale -> ActionPalette(
        core = Color(0xFFDDEBFF),
        edge = Color(0xFF88B4FF),
        glow = Color(0xFF79A1F7),
        ring = Color(0xFFA7C4FF),
    )
    BreathAction.HoldOut -> ActionPalette(
        core = Color(0xFFE8F0EC),
        edge = Color(0xFFAEC7C2),
        glow = Color(0xFF96B8B4),
        ring = Color(0xFFC6D8D3),
    )
    BreathAction.Rest -> ActionPalette(
        core = Color(0xFFF7F1E7),
        edge = Color(0xFFD9CCB5),
        glow = Color(0xFFE6D7BB),
        ring = Color(0xFFF1E4CC),
    )
}
