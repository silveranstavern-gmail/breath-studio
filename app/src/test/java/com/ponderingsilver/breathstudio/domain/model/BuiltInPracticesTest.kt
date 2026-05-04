package com.ponderingsilver.breathstudio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BuiltInPracticesTest {
    @Test
    fun presetsUseExplicitStepSounds() {
        val presets = BuiltInPractices.all + BuiltInPractices.BoxBreathing

        presets.forEach { practice ->
            practice.stages.forEach { stage ->
                stage.cycle.steps.forEach { step ->
                    val explicitDefaultAllowed = practice.id == BuiltInPractices.Sounds.id && step.safeLabel == "Default"
                    if (!explicitDefaultAllowed) {
                        assertNotEquals(
                            "${practice.safeTitle} / ${stage.safeTitle} / ${step.safeLabel}",
                            StepSound.Default,
                            step.sound,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun soundsPresetAuditionsEveryStepSoundForTwoRounds() {
        val stage = BuiltInPractices.Sounds.stages.single()
        val steps = stage.cycle.steps

        assertEquals(PracticeStageTarget.Rounds(2), stage.target)
        assertEquals(
            listOf(StepSound.Default, StepSound.Inhale, StepSound.Exhale, StepSound.Hold, StepSound.Other1, StepSound.Other2),
            steps.map { it.sound },
        )
        assertEquals(
            listOf(4, 8, 8, 4, 4, 4),
            steps.map { it.durationSeconds },
        )
    }

    @Test
    fun presetSoundsMatchBreathPhaseLabels() {
        val presets = BuiltInPractices.all + BuiltInPractices.BoxBreathing

        presets.forEach { practice ->
            practice.stages.forEach { stage ->
                stage.cycle.steps.forEach { step ->
                    val label = step.safeLabel.lowercase()
                    val expectedSound = when {
                        "inhale" in label -> StepSound.Inhale
                        "hold" in label || "block" in label -> StepSound.Hold
                        "exhale" in label || "out" in label || "ease" in label -> StepSound.Exhale
                        "pause" in label -> StepSound.Other2
                        else -> null
                    }

                    if (expectedSound != null) {
                        assertEquals(
                            "${practice.safeTitle} / ${stage.safeTitle} / ${step.safeLabel}",
                            expectedSound,
                            step.sound,
                        )
                    }
                }
            }
        }
    }
}
