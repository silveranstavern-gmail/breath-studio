package com.ponderingsilver.breathstudio

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.SessionRunTarget
import com.ponderingsilver.breathstudio.domain.model.toAuthoredPracticeDefinition

data class CueSettings(
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
) {
    companion object {
        val Defaults = CueSettings(
            soundEnabled = true,
            hapticsEnabled = true,
        )
    }
}

data class SessionConfig(
    val practice: BreathPractice,
    val runTarget: SessionRunTarget,
    val cues: CueSettings,
    val visualMode: BreathingVisualMode,
    val authoredDefinition: AuthoredPracticeDefinition? = null,
) {
    val durationMinutes: Int
        get() = when (val target = runTarget) {
            is SessionRunTarget.Timed -> target.durationMinutes
            is SessionRunTarget.PracticeCycles -> ceilDiv(
                target.cycles * (authoredDefinition?.blocks?.sumOf {
                    (it as? AuthoredPracticeBlock.RepeatingCycle)?.cycle?.durationMillis ?: 0L
                } ?: 0L),
                60_000L,
            ).toInt().coerceAtLeast(1)
        }

    companion object {
        fun fromPractice(
            practice: BreathPractice,
            cues: CueSettings = CueSettings.Defaults,
            authoredDefinition: AuthoredPracticeDefinition? = null,
        ): SessionConfig {
            val def = authoredDefinition ?: practice.toAuthoredPracticeDefinition()
            
            // Prioritize the structural intent of the practice (cycles and rounds).
            // This ensures that presets with specific round counts (like 108) or 
            // multi-stage routines always run their intended path rather than 
            // falling back to a generic timed loop.
            val runTarget = SessionRunTarget.PracticeCycles(1)

            return SessionConfig(
                practice = practice,
                runTarget = runTarget,
                cues = cues,
                visualMode = practice.preferredVisualMode,
                authoredDefinition = def,
            )
        }

        private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
    }
}
