package com.ponderingsilver.breathstudio.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ponderingsilver.breathstudio.SessionConfig
import com.ponderingsilver.breathstudio.domain.model.PlayerSessionState
import com.ponderingsilver.breathstudio.domain.model.SessionStatus
import com.ponderingsilver.breathstudio.ui.components.SelectionPill
import com.ponderingsilver.breathstudio.ui.visuals.BreathingVisual

@Composable
fun PracticePlayerScreen(
    config: SessionConfig,
    onBack: () -> Unit,
    viewModel: PracticePlayerViewModel = viewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val cueController = rememberCueController(config.cues)

    LaunchedEffect(config) {
        viewModel.start(config)
    }

    LaunchedEffect(viewModel, cueController) {
        viewModel.cueEvents.collect { event ->
            cueController.onCue(event)
        }
    }

    val state = sessionState ?: return
    val accent = accentColorForPractice(config.practice.safeCategory)
    val phaseTint = colorFromHex(state.currentStep.colorHex)

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
                    onBack = onBack,
                )
                PlayerHero(
                    sessionState = state,
                    stepColor = phaseTint,
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
private fun PlayerTopBar(
    title: String,
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
        SelectionPill(text = title)
    }
}

@Composable
private fun PlayerHero(
    sessionState: PlayerSessionState,
    stepColor: Color,
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
                    text = when (sessionState.status) {
                        SessionStatus.Preparing -> "Get oriented"
                        SessionStatus.Complete -> "Practice complete"
                        else -> sessionState.currentStep.stageTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xD5F4EFE2),
                )
                Text(
                    text = "${sessionState.plan.practice.safeSubtitle} • ${totalMinutes.coerceAtLeast(1)} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xBEE0E9E7),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BreathingVisual(
                    stageProgress = sessionState.currentStageProgress,
                    stepColor = stepColor,
                    modifier = Modifier
                        .sizeIn(maxWidth = 360.dp, maxHeight = 360.dp)
                        .aspectRatio(1f),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(0.52f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when (sessionState.status) {
                            SessionStatus.Preparing -> "Get ready"
                            SessionStatus.Complete -> "Rest"
                            else -> sessionState.currentLabel
                        },
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFF9F4E9),
                    )
                    Text(
                        text = when (sessionState.status) {
                            SessionStatus.Preparing -> formatSeconds(sessionState.orientationRemainingMillis)
                            SessionStatus.Complete -> "Finished"
                            else -> formatSeconds(sessionState.remainingStepMillis)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFEFD7A8),
                    )
                    Text(
                        text = when (sessionState.status) {
                            SessionStatus.Preparing -> "First cue starts after orientation"
                            else -> "${sessionState.currentStep.stageTitle} • ${formatClock(sessionState.elapsedSessionMillis)} elapsed"
                        },
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
            title = "Sensory",
            value = buildString {
                append(if (soundEnabled) "Audio" else "Silent")
                append(" & ")
                append(if (hapticsEnabled) "Haptic" else "Still")
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
                    SessionStatus.Preparing -> "Start now"
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

private fun accentColorForPractice(category: String): Color = when (category) {
    "Calming" -> Color(0xFFD5B27A)
    "Energizing" -> Color(0xFF80D4D0)
    "Reset" -> Color(0xFF80D2AC)
    else -> Color(0xFF9CCBE9)
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun colorFromHex(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(Color(0xFF9CCBE9))
}

private fun formatSeconds(millis: Long): String {
    val seconds = ((millis + 999L) / 1_000L).coerceAtLeast(0L)
    return "${seconds}s"
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes == 1) "1 minute" else "$minutes minutes"
}
