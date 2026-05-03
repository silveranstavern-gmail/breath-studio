package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeRepositoryTest {
    @Test
    fun repositoryMergesSavedPracticesAndDropsBuiltInIdCollisions() = runBlocking {
        val savedDtos = MutableStateFlow(
            listOf(
                validDto(id = BuiltInPractices.BoxBreathing.safeId, title = "Shadow Box"),
                validDto(id = "custom-one", title = "Custom One"),
            ),
        )
        val repository = DefaultPracticeRepository(savedPracticeDtos = savedDtos)

        val practices = repository.practices.first()

        assertTrue(practices.any { it.safeId == BuiltInPractices.BoxBreathing.safeId && it.safeTitle != "Shadow Box" })
        assertTrue(practices.any { it.safeId == "custom-one" })
        assertEquals(practices.size, practices.map { it.safeId }.toSet().size)
    }

    @Test
    fun repositoryFallsBackWhenBuiltInsAndSavedPracticesAreEmptyOrInvalid() = runBlocking {
        val repository = DefaultPracticeRepository(
            builtInPractices = emptyList(),
            savedPracticeDtos = MutableStateFlow(
                listOf(validDto(id = "   ")),
            ),
        )

        val practices = repository.practices.first()

        assertEquals(listOf(BuiltInPractices.BoxBreathing.safeId), practices.map { it.safeId })
    }

    @Test
    fun dtoMapperCapsExcessiveStagesAndSteps() {
        val dto = validDto(
            blocks = List(80) {
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    target = AuthoredBlockTargetDto.repetitions(1),
                    cycle = AuthoredPracticeCycleDto(
                        steps = List(80) {
                            AuthoredPracticeStepDto(
                                actionName = BreathAction.Inhale.name,
                                durationMillis = 4_000L,
                            )
                        },
                    ),
                )
            },
        )

        val practice = requireNotNull(dto.toDomainOrNull())

        assertEquals(40, practice.stages.size)
        assertFalse(practice.stages.any { stage -> stage.cycle.steps.size > 40 })
    }

    @Test
    fun repositoryMapsSavedSequenceBlocksIntoRuntimePractices() = runBlocking {
        val repository = DefaultPracticeRepository(
            savedPracticeDtos = MutableStateFlow(
                listOf(
                    validDto(
                        id = "sequence-practice",
                        title = "Sequence Practice",
                        blocks = listOf(
                            AuthoredPracticeBlockDto(
                                kind = AuthoredPracticeBlockDto.KindSequence,
                                title = "Retention",
                                steps = listOf(
                                    AuthoredPracticeStepDto(
                                        actionName = BreathAction.Exhale.name,
                                        durationMillis = 400L,
                                    ),
                                    AuthoredPracticeStepDto(
                                        actionName = BreathAction.HoldOut.name,
                                        durationMillis = 60_000L,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val practice = repository.practices.first().first { it.safeId == "sequence-practice" }

        assertEquals("Retention", practice.stages.single().safeTitle)
        assertEquals(listOf(BreathAction.Exhale, BreathAction.HoldOut), practice.stages.single().cycle.steps.map { it.action })
        assertEquals(listOf(1, 60), practice.stages.single().cycle.steps.map { it.safeDurationSeconds })
    }

    @Test
    fun repositoryExposesSavedEntriesAsEditableLibraryItems() = runBlocking {
        val repository = DefaultPracticeRepository(
            savedPracticeDtos = MutableStateFlow(
                listOf(validDto(id = "editable-custom", title = "Editable Custom")),
            ),
        )

        val entry = repository.entries.first().first { it.practice.safeId == "editable-custom" }

        assertEquals(PracticeSource.Saved, entry.source)
        assertTrue(entry.canEdit)
        assertEquals("editable-custom", entry.authoredDefinition?.safeId)
    }

    private fun validDto(
        id: String = "custom",
        title: String = "Custom Practice",
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
    )
}
