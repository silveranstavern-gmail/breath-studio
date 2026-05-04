package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.PracticeStageTarget
import com.ponderingsilver.breathstudio.domain.model.StepSound
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthoredPracticeDtosTest {
    @Test
    fun mapperDropsPracticeWithBlankId() {
        val practice = validDto(id = "   ").toDomainOrNull()

        assertNull(practice)
    }

    @Test
    fun mapperDropsPracticeWithNoValidSteps() {
        val practice = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.repetitions(1),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 4_000L,
                                label = "  ",
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNull(practice)
    }

    @Test
    fun mapperSanitizesInvalidTextDurationRouteAndStageTarget() {
        val longTitle = "A".repeat(200)
        val practice = validDto(
            title = longTitle,
            defaultDurationMinutes = -20,
            preferredVisualModeName = "NotAVisual",
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    title = "",
                    target = AuthoredBlockTargetDto.repetitions(-10),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = -5L,
                                label = "Step",
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNotNull(practice)
        requireNotNull(practice)
        assertEquals(80, practice.safeTitle.length)
        assertEquals(1, practice.safeDefaultDurationMinutes)
        assertEquals(PracticeStageTarget.Rounds(1), practice.stages.first().target)
        assertEquals("Practice block", practice.stages.first().safeTitle)
        assertEquals(1, practice.stages.first().cycle.steps.first().safeDurationSeconds)
        assertEquals("Step", practice.stages.first().cycle.steps.first().safeLabel)
        assertEquals("#7ED9C8", practice.stages.first().cycle.steps.first().safeColorHex)
    }

    @Test
    fun mapperPreservesDurationStageTarget() {
        val practice = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.durationMillis(90_000L),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 10_000L,
                                label = "Exhale",
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNotNull(practice)
        assertEquals(
            PracticeStageTarget.DurationSeconds(90),
            requireNotNull(practice).stages.first().target,
        )
    }

    @Test
    fun mapperPreservesExplicitStepSound() {
        val practice = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.repetitions(1),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 4_000L,
                                label = "Custom cue",
                                soundName = StepSound.Other1.name,
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNotNull(practice)
        assertEquals(StepSound.Other1, requireNotNull(practice).stages.first().cycle.steps.first().resolvedSound)
    }

    @Test
    fun mapperPreservesExplicitDefaultStepSound() {
        val practice = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.repetitions(1),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 4_000L,
                                label = "Inhale",
                                soundName = StepSound.Default.name,
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNotNull(practice)
        assertEquals(StepSound.Default, requireNotNull(practice).stages.first().cycle.steps.first().resolvedSound)
    }

    @Test
    fun mapperFallsBackToDefaultSoundForUnknownStepSound() {
        val practice = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.repetitions(1),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 8_000L,
                                label = "Long Exhale",
                                soundName = "not-a-sound",
                            ),
                        ),
                    ),
                ),
            ),
        ).toDomainOrNull()

        assertNotNull(practice)
        assertEquals(StepSound.Default, requireNotNull(practice).stages.first().cycle.steps.first().resolvedSound)
    }

    private fun validDto(
        id: String = "custom",
        title: String = "Custom Practice",
        defaultDurationMinutes: Int = 5,
        preferredVisualModeName: String? = null,
        blocks: List<AuthoredPracticeBlockDto> = listOf(
            AuthoredPracticeBlockDto(
                kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                target = AuthoredBlockTargetDto.repetitions(1),
                cycle = AuthoredPracticeCycleDto(
                    steps = listOf(
                        AuthoredPracticeStepDto(
                            durationMillis = 4_000L,
                            label = "Inhale",
                        ),
                    ),
                ),
            ),
        ),
    ): AuthoredPracticeDto = AuthoredPracticeDto(
        id = id,
        title = title,
        blocks = blocks,
        preferredVisualModeName = preferredVisualModeName,
        defaultDurationMinutes = defaultDurationMinutes,
    )
}
