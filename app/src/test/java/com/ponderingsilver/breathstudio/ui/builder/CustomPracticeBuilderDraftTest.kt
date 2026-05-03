package com.ponderingsilver.breathstudio.ui.builder

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPracticeBuilderDraftTest {
    @Test
    fun draftBuildsRepeatingBlocksWithDurationAndRoundsTargets() {
        val definition = CustomPracticeBuilderDraft(
            title = "Evening Ladder",
            visualMode = BreathingVisualMode.SquareTracer,
            blocks = listOf(
                EditablePracticeBlock(
                    id = "block-1",
                    title = "Warmup",
                    targetMode = BuilderTargetMode.DurationMinutes,
                    targetValueInput = "10",
                    steps = listOf(
                        EditablePracticeStep("1", BreathAction.Inhale, "5"),
                        EditablePracticeStep("2", BreathAction.Exhale, "5"),
                    ),
                ),
                EditablePracticeBlock(
                    id = "block-2",
                    title = "Main set",
                    targetMode = BuilderTargetMode.Repetitions,
                    targetValueInput = "6",
                    steps = listOf(
                        EditablePracticeStep("3", BreathAction.Inhale, "6"),
                        EditablePracticeStep("4", BreathAction.HoldIn, "6"),
                        EditablePracticeStep("5", BreathAction.Exhale, "6"),
                        EditablePracticeStep("6", BreathAction.HoldOut, "6"),
                    ),
                ),
            ),
            selectedBlockId = "block-1",
        ).toAuthoredPracticeDefinitionOrNull { "custom-evening-ladder" }

        assertNotNull(definition)
        requireNotNull(definition)
        assertEquals("custom-evening-ladder", definition.safeId)
        assertEquals(BreathingVisualMode.SquareTracer, definition.preferredVisualMode)
        assertEquals(13, definition.safeDefaultDurationMinutes)
        assertEquals(2, definition.blocks.size)
        val firstBlock = definition.blocks[0] as AuthoredPracticeBlock.RepeatingCycle
        val secondBlock = definition.blocks[1] as AuthoredPracticeBlock.RepeatingCycle
        assertEquals(AuthoredBlockTarget.DurationMillis(600_000L), firstBlock.target)
        assertEquals(AuthoredBlockTarget.Repetitions(6), secondBlock.target)
        assertEquals(listOf(5_000L, 5_000L), firstBlock.cycle.steps.map { it.safeDurationMillis })
        assertEquals(listOf(6_000L, 6_000L, 6_000L, 6_000L), secondBlock.cycle.steps.map { it.safeDurationMillis })
    }

    @Test
    fun draftRejectsInvalidDurationInput() {
        val definition = CustomPracticeBuilderDraft(
            blocks = listOf(
                EditablePracticeBlock(
                    id = "block-1",
                    title = "Broken",
                    targetMode = BuilderTargetMode.Repetitions,
                    targetValueInput = "3",
                    steps = listOf(
                        EditablePracticeStep("1", BreathAction.Inhale, "abc"),
                    ),
                ),
            ),
            selectedBlockId = "block-1",
        ).toAuthoredPracticeDefinitionOrNull { "invalid" }

        assertNull(definition)
    }

    @Test
    fun draftRoundTripsFromExistingDefinitionAndPreservesPracticeIdOnSave() {
        val existing = AuthoredPracticeDefinition(
            id = "custom-box",
            title = "Custom Box",
            subtitle = "Existing",
            description = "Existing practice",
            category = "Custom",
            blocks = listOf(
                AuthoredPracticeBlock.RepeatingCycle(
                    title = "Settle",
                    cycle = AuthoredPracticeCycle(
                        steps = listOf(
                            AuthoredPracticeStep(BreathAction.Inhale, 4_000L),
                            AuthoredPracticeStep(BreathAction.Exhale, 6_000L),
                        ),
                    ),
                    target = AuthoredBlockTarget.Repetitions(8),
                ),
                AuthoredPracticeBlock.RepeatingCycle(
                    title = "Lengthen",
                    cycle = AuthoredPracticeCycle(
                        steps = listOf(
                            AuthoredPracticeStep(BreathAction.Inhale, 5_000L),
                            AuthoredPracticeStep(BreathAction.Exhale, 7_000L),
                        ),
                    ),
                    target = AuthoredBlockTarget.DurationMillis(300_000L),
                ),
            ),
            preferredVisualMode = BreathingVisualMode.Circle,
            defaultDurationMinutes = 5,
        )

        val draft = existing.toBuilderDraftOrNull()

        assertNotNull(draft)
        requireNotNull(draft)
        assertTrue(draft.hasExistingPractice)
        assertEquals("custom-box", draft.practiceId)
        assertEquals(2, draft.blocks.size)
        assertEquals("8", draft.blocks[0].targetValueInput)
        assertEquals("5", draft.blocks[1].targetValueInput)
        assertEquals(listOf("4", "6"), draft.blocks[0].steps.map { it.durationInput })

        val saved = draft.copy(title = "Refined Box").toAuthoredPracticeDefinitionOrNull { "new-id" }

        assertNotNull(saved)
        assertEquals("custom-box", requireNotNull(saved).safeId)
        assertEquals("Refined Box", saved.safeTitle)
    }

    @Test
    fun draftEstimatesBlockAndSessionDurations() {
        val draft = CustomPracticeBuilderDraft(
            blocks = listOf(
                EditablePracticeBlock(
                    id = "block-1",
                    title = "Rapid",
                    targetMode = BuilderTargetMode.Repetitions,
                    targetValueInput = "3",
                    steps = listOf(
                        EditablePracticeStep("1", BreathAction.Inhale, "1.5"),
                        EditablePracticeStep("2", BreathAction.Exhale, "2"),
                    ),
                ),
                EditablePracticeBlock(
                    id = "block-2",
                    title = "Recover",
                    targetMode = BuilderTargetMode.DurationMinutes,
                    targetValueInput = "2",
                    steps = listOf(
                        EditablePracticeStep("3", BreathAction.Inhale, "4"),
                        EditablePracticeStep("4", BreathAction.Exhale, "6"),
                    ),
                ),
            ),
            selectedBlockId = "block-1",
        )

        assertEquals(3_500L, draft.blocks[0].cycleDurationMillisOrNull())
        assertEquals(10_500L, draft.blocks[0].estimatedBlockDurationMillisOrNull())
        assertEquals(130_500L, draft.estimatedTotalDurationMillisOrNull())
        assertFalse(draft.summaryLabel().isBlank())
    }
}
