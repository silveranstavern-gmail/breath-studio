package com.ponderingsilver.breathstudio.data.practice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeRepositoryTest {
    @Test
    fun repositoryExposesOnlySavedPracticesAndDedupeById() = runBlocking {
        val savedDtos = MutableStateFlow(
            listOf(
                validDto(id = "custom-one", title = "Custom One"),
                validDto(id = "custom-one", title = "Custom One"),
            ),
        )
        val repository = DefaultPracticeRepository(savedPracticeDtos = savedDtos)

        val practices = repository.practices.first()

        assertTrue(practices.any { it.safeId == "custom-one" })
        assertEquals(1, practices.size)
    }

    @Test
    fun repositoryStartsEmptyWhenSavedPracticesAreEmptyOrInvalid() = runBlocking {
        val repository = DefaultPracticeRepository(
            savedPracticeDtos = MutableStateFlow(
                listOf(validDto(id = "   ")),
            ),
        )

        val practices = repository.practices.first()

        assertTrue(practices.isEmpty())
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
                                durationMillis = 4_000L,
                                label = "Step",
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
    fun repositoryExposesSavedEntriesAsEditableLibraryItems() = runBlocking {
        val repository = DefaultPracticeRepository(
            savedPracticeDtos = MutableStateFlow(
                listOf(validDto(id = "editable-custom", title = "Editable Custom")),
            ),
        )

        val entry = repository.entries.first().first { it.practice.safeId == "editable-custom" }

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
                            durationMillis = 4_000L,
                            label = "Step",
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
