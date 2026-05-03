package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import com.ponderingsilver.breathstudio.domain.model.toDomainPracticeOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

enum class PracticeSource {
    BuiltIn,
    Saved,
}

data class PracticeLibraryEntry(
    val practice: BreathPractice,
    val source: PracticeSource,
    val authoredDefinition: AuthoredPracticeDefinition? = null,
) {
    val canEdit: Boolean
        get() = source == PracticeSource.Saved && authoredDefinition != null
}

interface PracticeRepository {
    val entries: Flow<List<PracticeLibraryEntry>>

    val practices: Flow<List<BreathPractice>>
        get() = entries.map { libraryEntries -> libraryEntries.map { it.practice } }
}

class DefaultPracticeRepository(
    private val builtInPractices: List<BreathPractice> = BuiltInPractices.all,
    savedPracticeDtos: Flow<List<AuthoredPracticeDto>> = MutableStateFlow(emptyList()),
) : PracticeRepository {
    override val entries: Flow<List<PracticeLibraryEntry>> = savedPracticeDtos.map { savedDtos ->
        val safeBuiltIns = builtInPractices
            .mapNotNull { practice -> runCatching { practice.copy(id = practice.safeId) }.getOrNull() }
            .dedupeById()
        val builtInEntries = safeBuiltIns.map { practice ->
            PracticeLibraryEntry(
                practice = practice,
                source = PracticeSource.BuiltIn,
            )
        }
        val savedEntries = savedDtos
            .mapNotNull { dto ->
                val definition = dto.toAuthoredDefinitionOrNull() ?: return@mapNotNull null
                val practice = definition.toDomainPracticeOrNull() ?: return@mapNotNull null
                PracticeLibraryEntry(
                    practice = practice,
                    source = PracticeSource.Saved,
                    authoredDefinition = definition,
                )
            }
            .filterNot { saved -> safeBuiltIns.any { builtIn -> builtIn.safeId == saved.practice.safeId } }
            .dedupeByPracticeId()

        (builtInEntries + savedEntries).ifEmpty {
            listOf(
                PracticeLibraryEntry(
                    practice = BuiltInPractices.BoxBreathing,
                    source = PracticeSource.BuiltIn,
                ),
            )
        }
    }

    private fun List<BreathPractice>.dedupeById(): List<BreathPractice> {
        val seenIds = mutableSetOf<String>()
        return filter { practice ->
            seenIds.add(practice.safeId)
        }
    }

    private fun List<PracticeLibraryEntry>.dedupeByPracticeId(): List<PracticeLibraryEntry> {
        val seenIds = mutableSetOf<String>()
        return filter { entry ->
            seenIds.add(entry.practice.safeId)
        }
    }
}
