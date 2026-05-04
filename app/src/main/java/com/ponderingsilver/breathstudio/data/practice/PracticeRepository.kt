package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import com.ponderingsilver.breathstudio.domain.model.toDomainPracticeOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

data class PracticeLibraryEntry(
    val practice: BreathPractice,
    val authoredDefinition: AuthoredPracticeDefinition? = null,
) {
    val canEdit: Boolean
        get() = authoredDefinition != null
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
        val builtInEntries = builtInPractices.map { practice ->
            PracticeLibraryEntry(
                practice = practice,
                authoredDefinition = null,
            )
        }
        val savedEntries = savedDtos.mapNotNull { dto ->
            val definition = dto.toAuthoredDefinitionOrNull() ?: return@mapNotNull null
            val practice = definition.toDomainPracticeOrNull() ?: return@mapNotNull null
            PracticeLibraryEntry(
                practice = practice,
                authoredDefinition = definition,
            )
        }

        (builtInEntries + savedEntries).dedupeByPracticeId()
    }

    private fun List<PracticeLibraryEntry>.dedupeByPracticeId(): List<PracticeLibraryEntry> {
        val seenIds = mutableSetOf<String>()
        return filter { entry ->
            seenIds.add(entry.practice.safeId)
        }
    }
}

