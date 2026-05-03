package com.ponderingsilver.breathstudio.ui.player

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ponderingsilver.breathstudio.CueSettings

interface CueController {
    fun onCue(event: PlayerCueEvent)

    fun release()
}

@Composable
fun rememberCueController(cues: CueSettings): CueController {
    val hapticFeedback = LocalHapticFeedback.current
    val toneGenerator = remember(cues.soundEnabled) {
        if (cues.soundEnabled) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, ToneVolume)
            }.getOrNull()
        } else {
            null
        }
    }
    val controller = remember(cues, hapticFeedback, toneGenerator) {
        AndroidCueController(
            cues = cues,
            hapticFeedback = hapticFeedback,
            toneGenerator = toneGenerator,
        )
    }

    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }

    return controller
}

private class AndroidCueController(
    private val cues: CueSettings,
    private val hapticFeedback: HapticFeedback,
    private val toneGenerator: ToneGenerator?,
) : CueController {
    override fun onCue(event: PlayerCueEvent) {
        if (cues.hapticsEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (cues.soundEnabled) {
            when (event) {
                is PlayerCueEvent.StepStarted -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, StepToneDurationMillis)
                PlayerCueEvent.SessionCompleted -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, CompleteToneDurationMillis)
            }
        }
    }

    override fun release() {
        toneGenerator?.release()
    }
}

private const val ToneVolume = 55
private const val StepToneDurationMillis = 90
private const val CompleteToneDurationMillis = 120
