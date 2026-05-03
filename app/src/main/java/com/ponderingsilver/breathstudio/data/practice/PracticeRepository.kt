package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import com.ponderingsilver.breathstudio.domain.model.toDomainPracticeOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface PracticeRepository {
    val practices: Flow<List<BreathPractice>>
}

class DefaultPracticeRepository(
    private val builtInPractices: List<BreathPractice> = BuiltInPractices.all,
    savedPracticeDtos: Flow<List<AuthoredPracticeDto>> = MutableStateFlow(emptyList()),
) : PracticeRepository {
    override val practices: Flow<List<BreathPractice>> = savedPracticeDtos.map { savedDtos ->
        val safeBuiltIns = builtInPractices
            .mapNotNull { practice -> runCatching { practice.copy(id = practice.safeId) }.getOrNull() }
            .dedupeById()
        val savedPractices = savedDtos
            .mapNotNull { dto -> dto.toAuthoredDefinitionOrNull()?.toDomainPracticeOrNull() }
            .filterNot { saved -> safeBuiltIns.any { builtIn -> builtIn.safeId == saved.safeId } }
            .dedupeById()

        (safeBuiltIns + savedPractices).ifEmpty { listOf(BuiltInPractices.BoxBreathing) }
    }

    private fun List<BreathPractice>.dedupeById(): List<BreathPractice> {
        val seenIds = mutableSetOf<String>()
        return filter { practice ->
            seenIds.add(practice.safeId)
        }
    }
}
