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
            
            // If the definition has multiple blocks or blocks with specific repetition counts,
            // we should probably follow its structure rather than forcing a timed loop.
            // But for simple single-block practices, we usually want to loop for the default duration.
            val runTarget = if (authoredDefinition != null || practice.stages.any { it.rounds > 1 } || practice.stages.size > 1) {
                // For authored or multi-stage/multi-round practices, use the structural duration
                SessionRunTarget.PracticeCycles(1)
            } else {
                SessionRunTarget.Timed(practice.safeDefaultDurationMinutes)
            }

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
