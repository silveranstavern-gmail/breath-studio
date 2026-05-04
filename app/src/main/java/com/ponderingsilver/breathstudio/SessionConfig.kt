package com.ponderingsilver.breathstudio

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode

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
    val durationMinutes: Int,
    val cues: CueSettings,
    val visualMode: BreathingVisualMode,
    val authoredDefinition: AuthoredPracticeDefinition? = null,
) {
    companion object {
        fun fromPractice(
            practice: BreathPractice,
            cues: CueSettings = CueSettings.Defaults,
            authoredDefinition: AuthoredPracticeDefinition? = null,
        ): SessionConfig = SessionConfig(
            practice = practice,
            durationMinutes = practice.safeDefaultDurationMinutes,
            cues = cues,
            visualMode = practice.preferredVisualMode,
            authoredDefinition = authoredDefinition,
        )
    }
}
