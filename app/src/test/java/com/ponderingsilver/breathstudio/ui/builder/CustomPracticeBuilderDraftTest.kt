package com.ponderingsilver.breathstudio.ui.builder

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
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
            visualMode = BreathingVisualMode.Glow,
            blocks = listOf(
                EditablePracticeBlock(
                    id = "block-1",
                    title = "Warmup",
                    targetMode = BuilderTargetMode.DurationMinutes,
                    targetValueInput = "10",
                    steps = listOf(
                        EditablePracticeStep("1", "5", "Inhale"),
                        EditablePracticeStep("2", "5", "Exhale"),
                    ),
                ),
                EditablePracticeBlock(
                    id = "block-2",
                    title = "Main set",
                    targetMode = BuilderTargetMode.Repetitions,
                    targetValueInput = "6",
                    steps = listOf(
                        EditablePracticeStep("3", "6", "Inhale"),
                        EditablePracticeStep("4", "6", "Hold"),
                        EditablePracticeStep("5", "6", "Exhale"),
                        EditablePracticeStep("6", "6", "Hold"),
                    ),
                ),
            ),
            selectedBlockId = "block-1",
        ).toAuthoredPracticeDefinitionOrNull { "custom-evening-ladder" }

        assertNotNull(definition)
        requireNotNull(definition)
        assertEquals("custom-evening-ladder", definition.safeId)
        assertEquals(BreathingVisualMode.Glow, definition.preferredVisualMode)
        assertEquals(2, definition.blocks.size)
        val firstBlock = definition.blocks[0] as AuthoredPracticeBlock.RepeatingCycle
        val secondBlock = definition.blocks[1] as AuthoredPracticeBlock.RepeatingCycle
        assertEquals(AuthoredBlockTarget.Repetitions(60, 600_000L), firstBlock.target)
        assertEquals(AuthoredBlockTarget.Repetitions(6), secondBlock.target)
        assertEquals(listOf(5_000L, 5_000L), firstBlock.cycle.steps.map { it.safeDurationMillis })
        assertEquals(listOf(6_000L, 6_000L, 6_000L, 6_000L), secondBlock.cycle.steps.map { it.safeDurationMillis })
    }

    @Test
    fun draftRoundTripsFromExistingDefinitionAndPreservesPracticeIdOnSave() {
        val existing = AuthoredPracticeDefinition(
            id = "custom-box",
            title = "Custom Box",
            subtitle = "Existing",
            description = "Existing practice",
            category = "Calming",
            blocks = listOf(
                AuthoredPracticeBlock.RepeatingCycle(
                    title = "Settle",
                    cycle = AuthoredPracticeCycle(
                        steps = listOf(
                            AuthoredPracticeStep(4_000L, "Inhale"),
                            AuthoredPracticeStep(6_000L, "Exhale"),
                        ),
                    ),
                    target = AuthoredBlockTarget.Repetitions(8),
                ),
                AuthoredPracticeBlock.RepeatingCycle(
                    title = "Lengthen",
                    cycle = AuthoredPracticeCycle(
                        steps = listOf(
                            AuthoredPracticeStep(5_000L, "Inhale"),
                            AuthoredPracticeStep(7_000L, "Exhale"),
                        ),
                    ),
                    target = AuthoredBlockTarget.DurationMillis(300_000L),
                ),
            ),
            preferredVisualMode = BreathingVisualMode.Glow,
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
        assertEquals(BuilderTargetMode.DurationMinutes, draft.blocks[1].targetMode)

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
                        EditablePracticeStep("1", "1.5", "Inhale"),
                        EditablePracticeStep("2", "2", "Exhale"),
                    ),
                ),
                EditablePracticeBlock(
                    id = "block-2",
                    title = "Recover",
                    targetMode = BuilderTargetMode.DurationMinutes,
                    targetValueInput = "2",
                    steps = listOf(
                        EditablePracticeStep("3", "4", "Inhale"),
                        EditablePracticeStep("4", "6", "Exhale"),
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

    @Test
    fun draftPreservesCustomStepLabelsWhenSavingAndReloading() {
        val definition = CustomPracticeBuilderDraft(
            title = "Alt nostril",
            blocks = listOf(
                EditablePracticeBlock(
                    id = "block-1",
                    title = "Main",
                    targetMode = BuilderTargetMode.Repetitions,
                    targetValueInput = "4",
                    steps = listOf(
                        EditablePracticeStep("1", "4", "Left nostril inhale"),
                        EditablePracticeStep("2", "4", "Left nostril hold", "#AA7733"),
                        EditablePracticeStep("3", "4", "Right nostril exhale", "#3366AA"),
                    ),
                ),
            ),
            selectedBlockId = "block-1",
        ).toAuthoredPracticeDefinitionOrNull { "alt-nostril" }

        assertNotNull(definition)
        requireNotNull(definition)
        val steps = (definition.blocks.first() as AuthoredPracticeBlock.RepeatingCycle).cycle.steps
        assertEquals(
            listOf("Left nostril inhale", "Left nostril hold", "Right nostril exhale"),
            steps.map { it.label },
        )
        assertEquals("#3366AA", steps.last().safeColorHex)

        val reloadedDraft = definition.toBuilderDraftOrNull()
        assertNotNull(reloadedDraft)
        assertEquals(
            listOf("Left nostril inhale", "Left nostril hold", "Right nostril exhale"),
            requireNotNull(reloadedDraft).blocks.first().steps.map { it.label },
        )
        assertEquals("#3366AA", requireNotNull(reloadedDraft).blocks.first().steps.last().colorHex)
    }

    @Test
    fun sanitizeDurationInputKeepsSingleDecimalAndDigitsOnly() {
        assertEquals("12.34", sanitizeDurationInput("a12..b3.4"))
        assertEquals("", sanitizeDurationInput("   "))
    }

    @Test
    fun adjustDurationInputBySecondsHandlesInvalidAndBounds() {
        assertEquals("2", adjustDurationInputBySeconds("1", 1))
        assertEquals("0.1", adjustDurationInputBySeconds("0.1", -1))
        assertEquals("2", adjustDurationInputBySeconds("bad", 1))
        assertEquals("3600", adjustDurationInputBySeconds("999999", 1))
    }
}
