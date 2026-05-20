package com.ponderingsilver.breathstudio

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition

sealed interface BreathStudioRoute {
    data object Home : BreathStudioRoute
    data object PresetPicker : BreathStudioRoute
    data object Support : BreathStudioRoute
    data object SupportDiagnostics : BreathStudioRoute
    data class Builder(val initialDefinition: AuthoredPracticeDefinition?) : BreathStudioRoute
    data class Player(val config: SessionConfig) : BreathStudioRoute
}
