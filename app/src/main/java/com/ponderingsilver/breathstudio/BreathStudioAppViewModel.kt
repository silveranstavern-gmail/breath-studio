package com.ponderingsilver.breathstudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ponderingsilver.breathstudio.data.practice.PracticeLibraryEntry
import com.ponderingsilver.breathstudio.data.practice.AuthoredPracticeDto
import com.ponderingsilver.breathstudio.data.practice.toAuthoredDto
import com.ponderingsilver.breathstudio.data.preferences.UserPreferences
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class BreathStudioAppState(
    val route: BreathStudioRoute = BreathStudioRoute.Home,
    val entries: List<PracticeLibraryEntry> = emptyList(),
    val selectedEntry: PracticeLibraryEntry? = null,
    val availablePresets: List<BreathPractice> = BuiltInPractices.all,
) {
    val practices: List<BreathPractice>
        get() = entries.map { entry -> entry.practice }
}

class BreathStudioAppViewModel(
    private val appContainer: BreathStudioAppContainer,
) : ViewModel() {
    private val route = MutableStateFlow<BreathStudioRoute>(BreathStudioRoute.Home)

    val appState: StateFlow<BreathStudioAppState> = combine(
        route,
        appContainer.userPreferencesRepository.preferences,
        appContainer.practiceRepository.entries,
    ) { currentRoute, userPreferences, libraryEntries ->
        val selectedEntry = libraryEntries.selectedEntryFor(userPreferences)
        BreathStudioAppState(
            route = currentRoute,
            entries = libraryEntries,
            selectedEntry = selectedEntry,
            availablePresets = BuiltInPractices.all,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BreathStudioAppState(),
    )

    fun selectPractice(practice: BreathPractice) {
        viewModelScope.launch {
            appContainer.userPreferencesRepository.setSelectedPracticeId(practice.safeId)
        }
    }

    fun startSelectedPractice() {
        val selectedEntry = appState.value.selectedEntry ?: return
        startPractice(selectedEntry.practice)
    }

    fun startPractice(practice: BreathPractice) {
        val entry = appState.value.entries.firstOrNull { libraryEntry ->
            libraryEntry.practice.safeId == practice.safeId
        }
        route.value = BreathStudioRoute.Player(
            config = SessionConfig.fromPractice(
                practice = entry?.practice ?: practice,
                authoredDefinition = entry?.authoredDefinition,
            ),
        )
    }

    fun openPresetPicker() {
        route.value = BreathStudioRoute.PresetPicker
    }

    fun openSupport() {
        route.value = BreathStudioRoute.Support
    }

    fun createCustomPractice() {
        route.value = BreathStudioRoute.Builder(initialDefinition = null)
    }

    fun editSelectedPractice() {
        val authoredDefinition = appState.value.selectedEntry?.authoredDefinition ?: return
        editPractice(authoredDefinition.safeId)
    }

    fun editPractice(practiceId: String) {
        val authoredDefinition = appState.value.entries
            .firstOrNull { entry -> entry.practice.safeId == practiceId }
            ?.authoredDefinition
            ?: return
        route.value = BreathStudioRoute.Builder(
            initialDefinition = authoredDefinition,
        )
    }

    fun copyPractice(practiceId: String) {
        val entry = appState.value.entries
            .firstOrNull { libraryEntry -> libraryEntry.practice.safeId == practiceId }
            ?: return
        viewModelScope.launch {
            val dto = entry.toTemplateCopyDto()
            val saved = appContainer.savedPracticeStore.upsertPractice(dto)
            if (saved) {
                appContainer.userPreferencesRepository.setSelectedPracticeId(dto.id)
                route.value = BreathStudioRoute.Home
            }
        }
    }

    fun addPresetToLibrary(practice: BreathPractice) {
        viewModelScope.launch {
            val dto = practice.toTemplateCopyDto()
            val saved = appContainer.savedPracticeStore.upsertPractice(dto)
            if (saved) {
                appContainer.userPreferencesRepository.setSelectedPracticeId(dto.id)
                route.value = BreathStudioRoute.Home
            }
        }
    }

    fun deleteSelectedPractice() {
        val selectedEntry = appState.value.selectedEntry ?: return
        val selectedId = selectedEntry.practice.safeId
        viewModelScope.launch {
            val deleted = appContainer.savedPracticeStore.deletePractice(selectedId)
            if (deleted) {
                val remainingEntries = appState.value.entries.filterNot { entry ->
                    entry.practice.safeId == selectedId
                }
                val nextSelection = remainingEntries.firstOrNull()?.practice?.safeId.orEmpty()
                appContainer.userPreferencesRepository.setSelectedPracticeId(nextSelection)
            }
        }
    }

    fun goHome() {
        route.value = BreathStudioRoute.Home
    }

    suspend fun savePractice(definition: AuthoredPracticeDefinition): Boolean {
        val saved = appContainer.savedPracticeStore.upsertPractice(definition.toAuthoredDto())
        if (saved) {
            appContainer.userPreferencesRepository.setSelectedPracticeId(definition.safeId)
        }
        return saved
    }

    private fun List<PracticeLibraryEntry>.selectedEntryFor(
        userPreferences: UserPreferences,
    ): PracticeLibraryEntry? {
        return firstOrNull { entry -> entry.practice.safeId == userPreferences.selectedPracticeId }
            ?: firstOrNull()
    }
}

private fun BreathPractice.toTemplateCopyDto(): AuthoredPracticeDto {
    val base = safeId
        .ifBlank { "preset" }
        .take(MaxTemplateBaseIdLength)
    val suffix = UUID.randomUUID().toString().replace("-", "").take(12)
    return toAuthoredDto().copy(id = "$base-$suffix")
}

private fun PracticeLibraryEntry.toTemplateCopyDto(): AuthoredPracticeDto {
    val base = practice.safeId
        .ifBlank { "practice" }
        .take(MaxTemplateBaseIdLength)
    val suffix = UUID.randomUUID().toString().replace("-", "").take(12)
    val dto = authoredDefinition?.toAuthoredDto() ?: practice.toAuthoredDto()
    return dto.copy(id = "$base-$suffix")
}

private const val MaxTemplateBaseIdLength = 96

class BreathStudioAppViewModelFactory(
    private val appContainer: BreathStudioAppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreathStudioAppViewModel::class.java)) {
            return BreathStudioAppViewModel(appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
