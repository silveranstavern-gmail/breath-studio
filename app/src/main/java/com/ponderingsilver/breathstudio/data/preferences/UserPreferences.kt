package com.ponderingsilver.breathstudio.data.preferences

import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices

data class UserPreferences(
    val selectedPracticeId: String = Defaults.SelectedPracticeId,
    val durationMinutes: Int = Defaults.DurationMinutes,
    val soundEnabled: Boolean = Defaults.SoundEnabled,
    val hapticsEnabled: Boolean = Defaults.HapticsEnabled,
    val visualModeOverride: BreathingVisualMode? = Defaults.VisualModeOverride,
) {
    companion object Defaults {
        val SelectedPracticeId: String = BuiltInPractices.BoxBreathing.safeId
        val DurationMinutes: Int = BuiltInPractices.BoxBreathing.safeDefaultDurationMinutes
        const val SoundEnabled: Boolean = true
        const val HapticsEnabled: Boolean = true
        val VisualModeOverride: BreathingVisualMode? = null
        const val MinimumDurationMinutes: Int = 1
        const val MaximumDurationMinutes: Int = 240
        const val MaximumPracticeIdLength: Int = 128
    }
}
