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
import kotlin.math.exp
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
                    durationMillis = DefaultCueMillis,
                    frequencies = doubleArrayOf(261.63, 392.0, 523.25, 659.25),
                    volume = BellToneVolume,
                    envelope = ::bellEnvelope,
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
                volume = BellToneVolume,
                envelope = ::bellEnvelope,
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
    val profile = if (inhale) InhaleBreathProfile else ExhaleBreathProfile
    val bodyNoise = BandPassNoise()
    val mouthNoise = BandPassNoise()
    val airNoise = BandPassNoise()
    var pinkNoise = 0.0
    var flutterPhase = 0.0

    repeat(sampleCount) { index ->
        val progress = index.toDouble() / sampleCount.toDouble()
        val noise = random.nextDouble() * 2.0 - 1.0
        pinkNoise += (noise - pinkNoise) * PinkNoiseSmoothing

        val motion = if (inhale) smoothStep(progress) else 1.0 - smoothStep(progress)
        val body = bodyNoise.process(
            input = pinkNoise,
            lowCut = lerp(profile.bodyLowCutStart, profile.bodyLowCutEnd, motion),
            highCut = lerp(profile.bodyHighCutStart, profile.bodyHighCutEnd, motion),
        )
        val mouth = mouthNoise.process(
            input = noise,
            lowCut = lerp(profile.mouthLowCutStart, profile.mouthLowCutEnd, motion),
            highCut = lerp(profile.mouthHighCutStart, profile.mouthHighCutEnd, motion),
        )
        val air = airNoise.process(
            input = noise,
            lowCut = lerp(profile.airLowCutStart, profile.airLowCutEnd, motion),
            highCut = lerp(profile.airHighCutStart, profile.airHighCutEnd, motion),
        )

        flutterPhase += 2.0 * PI * profile.flutterFrequency / SampleRate
        val flutter = 1.0 + profile.flutterDepth * sin(flutterPhase)
        val breathEnvelope = profile.envelope(progress)
        val edgeFade = fadeEnvelope(progress)
        val breathSignal = softSaturate(body * profile.bodyMix + mouth * profile.mouthMix + air * profile.airMix)
        val sample = breathSignal * profile.volume * breathEnvelope * edgeFade * flutter
        bytes.writeSample(index, sample)
    }
    return bytes
}

private class BandPassNoise {
    private var highPassed = 0.0
    private var bandLimited = 0.0

    fun process(input: Double, lowCut: Double, highCut: Double): Double {
        highPassed += (input - highPassed) * onePoleCoefficient(lowCut)
        val bright = input - highPassed
        bandLimited += (bright - bandLimited) * onePoleCoefficient(highCut) * BreathFilterDamping
        return bandLimited
    }
}

private data class BreathProfile(
    val volume: Double,
    val bodyMix: Double,
    val mouthMix: Double,
    val airMix: Double,
    val bodyLowCutStart: Double,
    val bodyLowCutEnd: Double,
    val bodyHighCutStart: Double,
    val bodyHighCutEnd: Double,
    val mouthLowCutStart: Double,
    val mouthLowCutEnd: Double,
    val mouthHighCutStart: Double,
    val mouthHighCutEnd: Double,
    val airLowCutStart: Double,
    val airLowCutEnd: Double,
    val airHighCutStart: Double,
    val airHighCutEnd: Double,
    val flutterFrequency: Double,
    val flutterDepth: Double,
    val envelope: (Double) -> Double,
)

private fun generateTone(
    durationMillis: Long,
    frequencies: DoubleArray,
    volume: Double,
    envelope: (Double) -> Double = ::fadeEnvelope,
): ByteArray {
    val sampleCount = ((durationMillis * SampleRate) / 1_000L).toInt().coerceAtLeast(1)
    val bytes = ByteArray(sampleCount * BytesPerSample)

    repeat(sampleCount) { index ->
        val progress = index.toDouble() / sampleCount.toDouble()
        val amplitude = envelope(progress)
        val mixed = frequencies.sumOf { frequency ->
            sin(2.0 * PI * frequency * index.toDouble() / SampleRate)
        } / frequencies.size.toDouble()
        bytes.writeSample(index, mixed * volume * amplitude)
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

private fun inhaleBreathEnvelope(progress: Double): Double {
    return 0.16 + 0.84 * smoothStep(progress)
}

private fun exhaleBreathEnvelope(progress: Double): Double {
    return 1.0 - 0.54 * smoothStep(progress)
}

private fun softSaturate(value: Double): Double {
    return value / (1.0 + kotlin.math.abs(value))
}

private fun fadeEnvelope(progress: Double): Double {
    val fadeIn = (progress / FadePortion).coerceIn(0.0, 1.0)
    val fadeOut = ((1.0 - progress) / FadePortion).coerceIn(0.0, 1.0)
    return minOf(fadeIn, fadeOut)
}

private fun bellEnvelope(progress: Double): Double {
    val attack = (progress / BellAttackPortion).coerceIn(0.0, 1.0)
    val decay = exp(-BellDecayRate * progress)
    val release = ((1.0 - progress) / BellReleasePortion).coerceIn(0.0, 1.0)
    return attack * decay * release
}

private fun onePoleCoefficient(frequency: Double): Double {
    return (1.0 - exp(-2.0 * PI * frequency / SampleRate.toDouble())).coerceIn(0.001, 0.99)
}

private fun lerp(start: Double, end: Double, progress: Double): Double {
    return start + (end - start) * progress.coerceIn(0.0, 1.0)
}

private const val SampleRate = 22_050
private const val BytesPerSample = 2
private const val MinimumGeneratedMillis = 120L
private const val MaximumGeneratedMillis = 120_000L
private const val SoftCueMillis = 760L
private const val DefaultCueMillis = 1_250L
private const val CompletionCueMillis = 1_100L
private const val FadePortion = 0.08
private const val BellAttackPortion = 0.02
private const val BellReleasePortion = 0.12
private const val BellDecayRate = 4.4
private const val PinkNoiseSmoothing = 0.010
private const val BreathFilterDamping = 0.58
private const val BellToneVolume = 0.16
private const val SoftToneVolume = 0.18
private const val SofterToneVolume = 0.14

private val InhaleBreathProfile = BreathProfile(
    volume = 0.12,
    bodyMix = 0.38,
    mouthMix = 0.52,
    airMix = 0.10,
    bodyLowCutStart = 190.0,
    bodyLowCutEnd = 260.0,
    bodyHighCutStart = 620.0,
    bodyHighCutEnd = 820.0,
    mouthLowCutStart = 520.0,
    mouthLowCutEnd = 780.0,
    mouthHighCutStart = 1_320.0,
    mouthHighCutEnd = 2_050.0,
    airLowCutStart = 1_650.0,
    airLowCutEnd = 2_150.0,
    airHighCutStart = 3_100.0,
    airHighCutEnd = 4_100.0,
    flutterFrequency = 0.72,
    flutterDepth = 0.020,
    envelope = ::inhaleBreathEnvelope,
)

private val ExhaleBreathProfile = BreathProfile(
    volume = 0.109,
    bodyMix = 0.48,
    mouthMix = 0.46,
    airMix = 0.06,
    bodyLowCutStart = 150.0,
    bodyLowCutEnd = 210.0,
    bodyHighCutStart = 500.0,
    bodyHighCutEnd = 700.0,
    mouthLowCutStart = 360.0,
    mouthLowCutEnd = 560.0,
    mouthHighCutStart = 1_000.0,
    mouthHighCutEnd = 1_520.0,
    airLowCutStart = 1_300.0,
    airLowCutEnd = 1_800.0,
    airHighCutStart = 2_600.0,
    airHighCutEnd = 3_450.0,
    flutterFrequency = 0.48,
    flutterDepth = 0.016,
    envelope = ::exhaleBreathEnvelope,
)
