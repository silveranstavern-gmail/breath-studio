package com.ponderingsilver.breathstudio.data.preferences

data class UserPreferences(
    val selectedPracticeId: String = Defaults.SelectedPracticeId,
) {
    companion object Defaults {
        const val SelectedPracticeId: String = ""
        const val MaximumPracticeIdLength: Int = 128
    }
}
