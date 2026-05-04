package com.ponderingsilver.breathstudio.ui.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ponderingsilver.breathstudio.CueSettings
import com.ponderingsilver.breathstudio.domain.model.StepSound
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

interface CueController {
    fun onCue(event: PlayerCueEvent)

    fun release()
}

@Composable
fun rememberCueController(cues: CueSettings): CueController {
    val hapticFeedback = LocalHapticFeedback.current
    val soundPlayer = remember(cues.soundEnabled) {
        if (cues.soundEnabled) BreathSoundPlayer() else null
    }
    val controller = remember(cues, hapticFeedback, soundPlayer) {
        AndroidCueController(
            cues = cues,
            hapticFeedback = hapticFeedback,
            soundPlayer = soundPlayer,
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
    private val soundPlayer: BreathSoundPlayer?,
) : CueController {
    override fun onCue(event: PlayerCueEvent) {
        if (cues.hapticsEnabled && event != PlayerCueEvent.StepStopped) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (!cues.soundEnabled) return

        when (event) {
            is PlayerCueEvent.StepStarted -> soundPlayer?.playStepSound(
                sound = event.step.sound,
                durationMillis = event.step.durationMillis,
            )
            PlayerCueEvent.StepStopped -> soundPlayer?.stop()
            PlayerCueEvent.SessionCompleted -> soundPlayer?.playCompletion()
        }
    }

    override fun release() {
        soundPlayer?.release()
    }
}

private class BreathSoundPlayer {
    private val lock = Any()
    private var activeTrack: AudioTrack? = null

    fun playStepSound(sound: StepSound, durationMillis: Long) {
        val safeDurationMillis = durationMillis.coerceIn(MinimumGeneratedMillis, MaximumGeneratedMillis)
        playAsync {
            when (sound) {
                StepSound.Default -> generateTone(
                    durationMillis = SoftCueMillis,
                    frequencies = doubleArrayOf(392.0),
                    volume = SoftToneVolume,
                )
                StepSound.Inhale -> generateBreath(
                    durationMillis = safeDurationMillis,
                    inhale = true,
                )
                StepSound.Exhale -> generateBreath(
                    durationMillis = safeDurationMillis,
                    inhale = false,
                )
                StepSound.Hold -> generateTone(
                    durationMillis = SoftCueMillis,
                    frequencies = doubleArrayOf(440.0, 554.37),
                    volume = SoftToneVolume,
                )
                StepSound.Other1 -> generateTone(
                    durationMillis = SoftCueMillis,
                    frequencies = doubleArrayOf(329.63, 493.88),
                    volume = SoftToneVolume,
                )
                StepSound.Other2 -> generateTone(
                    durationMillis = SoftCueMillis,
                    frequencies = doubleArrayOf(587.33, 783.99),
                    volume = SofterToneVolume,
                )
            }
        }
    }

    fun playCompletion() {
        playAsync {
            generateTone(
                durationMillis = CompletionCueMillis,
                frequencies = doubleArrayOf(392.0, 523.25, 659.25),
                volume = SoftToneVolume,
            )
        }
    }

    fun stop() {
        synchronized(lock) {
            activeTrack?.stopSafely()
            activeTrack?.release()
            activeTrack = null
        }
    }

    fun release() {
        stop()
    }

    private fun playAsync(generator: () -> ByteArray) {
        thread(name = "breath-step-sound", isDaemon = true) {
            val audioBytes = generator()
            val track = buildStaticTrack(audioBytes) ?: return@thread
            synchronized(lock) {
                activeTrack?.stopSafely()
                activeTrack?.release()
                activeTrack = track
            }
            track.play()
        }
    }

    private fun buildStaticTrack(audioBytes: ByteArray): AudioTrack? {
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(audioBytes.size)
                .build()
                .also { track -> track.write(audioBytes, 0, audioBytes.size) }
        }.getOrNull()
    }
}

private fun generateBreath(
    durationMillis: Long,
    inhale: Boolean,
): ByteArray {
    val sampleCount = ((durationMillis * SampleRate) / 1_000L).toInt().coerceAtLeast(1)
    val bytes = ByteArray(sampleCount * BytesPerSample)
    val random = Random(if (inhale) 17L else 29L)
    var filteredNoise = 0.0
    var phase = 0.0

    repeat(sampleCount) { index ->
        val progress = index.toDouble() / sampleCount.toDouble()
        val noise = random.nextDouble() * 2.0 - 1.0
        val smoothing = if (inhale) 0.035 + (progress * 0.02) else 0.055 - (progress * 0.02)
        filteredNoise += (noise - filteredNoise) * smoothing.coerceIn(0.02, 0.08)

        val baseFrequency = if (inhale) {
            155.0 + progress * 95.0
        } else {
            245.0 - progress * 105.0
        }
        phase += 2.0 * PI * baseFrequency / SampleRate

        val breathEnvelope = if (inhale) {
            0.18 + 0.82 * smoothStep(progress)
        } else {
            1.0 - 0.62 * smoothStep(progress)
        }
        val edgeFade = fadeEnvelope(progress)
        val airySignal = filteredNoise * 0.78 + sin(phase) * 0.055
        val sample = airySignal * BreathVolume * breathEnvelope * edgeFade
        bytes.writeSample(index, sample)
    }
    return bytes
}

private fun generateTone(
    durationMillis: Long,
    frequencies: DoubleArray,
    volume: Double,
): ByteArray {
    val sampleCount = ((durationMillis * SampleRate) / 1_000L).toInt().coerceAtLeast(1)
    val bytes = ByteArray(sampleCount * BytesPerSample)

    repeat(sampleCount) { index ->
        val progress = index.toDouble() / sampleCount.toDouble()
        val envelope = fadeEnvelope(progress)
        val mixed = frequencies.sumOf { frequency ->
            sin(2.0 * PI * frequency * index.toDouble() / SampleRate)
        } / frequencies.size.toDouble()
        bytes.writeSample(index, mixed * volume * envelope)
    }
    return bytes
}

private fun ByteArray.writeSample(index: Int, sample: Double) {
    val pcm = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
    val byteIndex = index * BytesPerSample
    this[byteIndex] = (pcm.toInt() and 0xFF).toByte()
    this[byteIndex + 1] = ((pcm.toInt() shr 8) and 0xFF).toByte()
}

private fun AudioTrack.stopSafely() {
    runCatching {
        if (playState == AudioTrack.PLAYSTATE_PLAYING) stop()
    }
}

private fun smoothStep(value: Double): Double {
    val bounded = value.coerceIn(0.0, 1.0)
    return bounded * bounded * (3.0 - 2.0 * bounded)
}

private fun fadeEnvelope(progress: Double): Double {
    val fadeIn = (progress / FadePortion).coerceIn(0.0, 1.0)
    val fadeOut = ((1.0 - progress) / FadePortion).coerceIn(0.0, 1.0)
    return minOf(fadeIn, fadeOut)
}

private const val SampleRate = 22_050
private const val BytesPerSample = 2
private const val MinimumGeneratedMillis = 120L
private const val MaximumGeneratedMillis = 120_000L
private const val SoftCueMillis = 760L
private const val CompletionCueMillis = 1_100L
private const val FadePortion = 0.08
private const val BreathVolume = 0.26
private const val SoftToneVolume = 0.18
private const val SofterToneVolume = 0.14
