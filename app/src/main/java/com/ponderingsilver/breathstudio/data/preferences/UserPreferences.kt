package com.ponderingsilver.breathstudio.data.preferences

import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices

data class UserPreferences(
    val selectedPracticeId: String = Defaults.SelectedPracticeId,
) {
    companion object Defaults {
        val SelectedPracticeId: String = BuiltInPractices.BoxBreathing.safeId
        const val MaximumPracticeIdLength: Int = 128
    }
}
