package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathRoute
import com.ponderingsilver.breathstudio.domain.model.PracticeStageTarget
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
                                actionName = "NotARealAction",
                                durationMillis = 4_000L,
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
                                actionName = BreathAction.Inhale.name,
                                durationMillis = -5L,
                                label = "",
                                routeName = "BogusRoute",
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
        assertEquals(BreathAction.Inhale.label, practice.stages.first().cycle.steps.first().safeLabel)
        assertEquals(BreathRoute.Both, practice.stages.first().cycle.steps.first().route)
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
                                actionName = BreathAction.Exhale.name,
                                durationMillis = 10_000L,
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
    fun mapperPreservesSequenceBlockAndSubSecondDurationsInAuthoredDefinition() {
        val definition = validDto(
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindSequence,
                    title = "Retention",
                    steps = listOf(
                        AuthoredPracticeStepDto(
                            actionName = BreathAction.Exhale.name,
                            durationMillis = 400L,
                            label = "Exhale fully",
                            routeName = BreathRoute.Left.name,
                        ),
                        AuthoredPracticeStepDto(
                            actionName = BreathAction.HoldOut.name,
                            durationMillis = 60_000L,
                            routeName = BreathRoute.Right.name,
                        ),
                    ),
                ),
            ),
        ).toAuthoredDefinitionOrNull()

        assertNotNull(definition)
        val block = requireNotNull(definition).blocks.single() as com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock.Sequence
        assertEquals(listOf(400L, 60_000L), block.steps.map { it.safeDurationMillis })
        assertEquals(listOf(BreathRoute.Left, BreathRoute.Right), block.steps.map { it.route })
        assertTrue(block.steps.first().safeLabel.contains("Exhale"))
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
                            actionName = BreathAction.Inhale.name,
                            durationMillis = 4_000L,
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
