package com.ponderingsilver.breathstudio.ui.player

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ponderingsilver.breathstudio.SessionConfig
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.PlayerSessionState
import com.ponderingsilver.breathstudio.domain.model.SessionStatus
import com.ponderingsilver.breathstudio.ui.visuals.BreathingVisual

@Composable
fun PracticePlayerScreen(
    config: SessionConfig,
    onBack: () -> Unit,
    viewModel: PracticePlayerViewModel = viewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val toneGenerator = rememberCueToneGenerator()

    LaunchedEffect(config) {
        viewModel.start(config)
    }

    LaunchedEffect(viewModel, config.cues.soundEnabled, config.cues.hapticsEnabled, toneGenerator) {
        viewModel.cueEvents.collect { event ->
            if (config.cues.hapticsEnabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (config.cues.soundEnabled) {
                when (event) {
                    is PlayerCueEvent.StepStarted -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
                    PlayerCueEvent.SessionCompleted -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                }
            }
        }
    }

    val state = sessionState ?: return
    val accent = accentColorForPractice(config.practice.safeCategory)
    val phaseTint = accentForAction(state.currentAction)

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF08161D),
                            accent.copy(alpha = 0.28f),
                            Color(0xFF11242A),
                        ),
                    ),
                )
                .padding(innerPadding)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(phaseTint.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.32f),
                            radius = size.minDimension * 0.56f,
                        ),
                        radius = size.minDimension * 0.56f,
                        center = Offset(size.width / 2f, size.height * 0.32f),
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PlayerTopBar(
                    title = config.practice.safeTitle,
                    visualMode = config.visualMode,
                    onBack = onBack,
                )
                PlayerHero(
                    sessionState = state,
                    visualMode = config.visualMode,
                    accent = accent,
                    totalMinutes = config.durationMinutes.coerceAtLeast(1),
                    modifier = Modifier.weight(1f),
                )
                PlayerStats(
                    sessionState = state,
                    totalMinutes = config.durationMinutes.coerceAtLeast(1),
                    soundEnabled = config.cues.soundEnabled,
                    hapticsEnabled = config.cues.hapticsEnabled,
                )
                PlayerControls(
                    sessionState = state,
                    onPauseResume = viewModel::pauseOrResume,
                    onReset = viewModel::restart,
                    onFinish = onBack,
                )
            }
        }
    }
}

@Composable
private fun rememberCueToneGenerator(): ToneGenerator? {
    val toneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, 55)
        }.getOrNull()
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator?.release()
        }
    }

    return toneGenerator
}

@Composable
private fun PlayerTopBar(
    title: String,
    visualMode: BreathingVisualMode,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("Back", style = MaterialTheme.typography.titleMedium)
        }
        SelectionPill(text = "$title • ${visualMode.label}")
    }
}

@Composable
private fun PlayerHero(
    sessionState: PlayerSessionState,
    visualMode: BreathingVisualMode,
    accent: Color,
    totalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x10FFFFFF),
            contentColor = Color(0xFFF7F2E7),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (sessionState.status == SessionStatus.Complete) "Practice complete" else sessionState.currentStep.stageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xD5F4EFE2),
                )
                Text(
                    text = "${sessionState.plan.practice.safeSubtitle} • ${totalMinutes.coerceAtLeast(1)} min • ${visualMode.label} guide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xBEE0E9E7),
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val maxVisualSize = minOf(maxWidth, 360.dp)
                BreathingVisual(
                    visualMode = visualMode,
                    action = sessionState.currentAction,
                    stepProgress = sessionState.currentStepProgress,
                    cycleProgress = sessionState.currentCycleProgress,
                    sessionProgress = sessionState.sessionProgress,
                    accent = accent,
                    modifier = Modifier.size(maxVisualSize),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(0.52f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (sessionState.status == SessionStatus.Complete) "Rest" else sessionState.currentLabel,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFF9F4E9),
                    )
                    Text(
                        text = if (sessionState.status == SessionStatus.Complete) "Finished" else formatSeconds(sessionState.remainingStepMillis),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFEFD7A8),
                    )
                    Text(
                        text = "${sessionState.currentStep.stageTitle} • ${formatClock(sessionState.elapsedSessionMillis)} elapsed",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xCCE3EBE8),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerStats(
    sessionState: PlayerSessionState,
    totalMinutes: Int,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Remaining",
            value = formatClock(sessionState.remainingSessionMillis),
            caption = "of ${formatMinutes(totalMinutes)}",
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Cues",
            value = buildString {
                append(if (soundEnabled) "Tone" else "Silent")
                append(" + ")
                append(if (hapticsEnabled) "Touch" else "Still")
            },
            caption = sessionState.plan.practice.safeCategory,
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x12FFFFFF),
            contentColor = Color(0xFFF7F1E4),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xC6DCE9E6),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 32.sp,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xB8D6E4E1),
            )
        }
    }
}

@Composable
private fun PlayerControls(
    sessionState: PlayerSessionState,
    onPauseResume: () -> Unit,
    onReset: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onPauseResume,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFECD6A8),
                contentColor = Color(0xFF163338),
            ),
        ) {
            Text(
                text = when (sessionState.status) {
                    SessionStatus.Running -> "Pause practice"
                    SessionStatus.Paused -> "Resume practice"
                    SessionStatus.Complete -> "Start again"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text("Restart")
            }
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(if (sessionState.status == SessionStatus.Complete) "Back to practices" else "End practice")
            }
        }
    }
}

@Composable
private fun SelectionPill(text: String) {
    Surface(
        color = Color(0x18FFFFFF),
        contentColor = Color(0xFFEFDDB4),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun accentColorForPractice(category: String): Color = when (category) {
    "Focus" -> Color(0xFF80D4D0)
    "Sleep" -> Color(0xFFD5B27A)
    "Reset" -> Color(0xFF80D2AC)
    else -> Color(0xFF9CCBE9)
}

private fun accentForAction(action: BreathAction): Color = when (action) {
    BreathAction.Inhale -> Color(0xFF7ED9C8)
    BreathAction.HoldIn -> Color(0xFFE7C98C)
    BreathAction.Exhale -> Color(0xFF9BC1FF)
    BreathAction.HoldOut -> Color(0xFFC7D7D4)
    BreathAction.Rest -> Color(0xFFE8E1D1)
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSeconds(millis: Long): String {
    val seconds = ((millis + 999L) / 1_000L).coerceAtLeast(0L)
    return "${seconds}s"
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes == 1) "1 minute" else "$minutes minutes"
}
