package com.ponderingsilver.breathstudio

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition

sealed interface BreathStudioRoute {
    data object Home : BreathStudioRoute
    data class Builder(val initialDefinition: AuthoredPracticeDefinition?) : BreathStudioRoute
    data class Player(val config: SessionConfig) : BreathStudioRoute
}
