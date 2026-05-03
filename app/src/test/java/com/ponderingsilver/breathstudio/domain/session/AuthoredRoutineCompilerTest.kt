package com.ponderingsilver.breathstudio.domain.session

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathRoute
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthoredRoutineCompilerTest {
    @Test
    fun repeatingCycleBlockExpandsByRepetitionCount() {
        val steps = buildTemplateSteps(
            definition = testDefinition(
                blocks = listOf(
                    AuthoredPracticeBlock.RepeatingCycle(
                        title = "Box",
                        cycle = AuthoredPracticeCycle(
                            steps = listOf(
                                AuthoredPracticeStep(BreathAction.Inhale, durationMillis = 1_000L),
                                AuthoredPracticeStep(BreathAction.Exhale, durationMillis = 2_000L),
                            ),
                        ),
                        target = AuthoredBlockTarget.Repetitions(2),
                    ),
                ),
            ),
        )

        assertEquals(4, steps.size)
        assertEquals(listOf(1_000L, 2_000L, 1_000L, 2_000L), steps.map { it.durationMillis })
        assertEquals(listOf(0, 0, 1, 1), steps.map { it.roundInStage })
    }

    @Test
    fun timedRepeatingBlockRoundsUpToWholeCycles() {
        val steps = buildTemplateSteps(
            definition = testDefinition(
                blocks = listOf(
                    AuthoredPracticeBlock.RepeatingCycle(
                        title = "Ladder",
                        cycle = AuthoredPracticeCycle(
                            steps = listOf(
                                AuthoredPracticeStep(BreathAction.Inhale, durationMillis = 500L),
                                AuthoredPracticeStep(BreathAction.Exhale, durationMillis = 500L),
                            ),
                        ),
                        target = AuthoredBlockTarget.DurationMillis(durationMillis = 2_500L),
                    ),
                ),
            ),
        )

        assertEquals(6, steps.size)
        assertEquals(3_000L, steps.sumOf { it.durationMillis })
        assertEquals(listOf(0, 0, 1, 1, 2, 2), steps.map { it.roundInStage })
    }

    @Test
    fun sequenceBlockRunsOnceAndPreservesRoutesAndSubSecondDurations() {
        val steps = buildTemplateSteps(
            definition = testDefinition(
                blocks = listOf(
                    AuthoredPracticeBlock.Sequence(
                        title = "Retention",
                        steps = listOf(
                            AuthoredPracticeStep(
                                action = BreathAction.Exhale,
                                durationMillis = 400L,
                                label = "Exhale fully",
                                route = BreathRoute.Left,
                            ),
                            AuthoredPracticeStep(
                                action = BreathAction.HoldOut,
                                durationMillis = 60_000L,
                                route = BreathRoute.Right,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(2, steps.size)
        assertEquals(listOf(400L, 60_000L), steps.map { it.durationMillis })
        assertEquals(listOf("Exhale fully", BreathAction.HoldOut.label), steps.map { it.label })
        assertEquals(listOf(BreathRoute.Left, BreathRoute.Right), steps.map { it.route })
        assertEquals(listOf(0, 0), steps.map { it.roundInStage })
    }

    private fun testDefinition(
        blocks: List<AuthoredPracticeBlock>,
    ): AuthoredPracticeDefinition = AuthoredPracticeDefinition(
        id = "authored-test",
        title = "Authored Test",
        subtitle = "Testing",
        description = "A routine used for authored compiler coverage.",
        category = "Test",
        blocks = blocks,
        preferredVisualMode = BreathingVisualMode.Circle,
    )
}
