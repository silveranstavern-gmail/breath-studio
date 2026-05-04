package com.ponderingsilver.breathstudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ponderingsilver.breathstudio.data.practice.PracticeLibraryEntry
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
            availablePresets = BuiltInPractices.all.filterNot { preset ->
                libraryEntries.any { entry -> entry.practice.safeId == preset.safeId }
            },
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
        val selectedPractice = appState.value.selectedEntry?.practice ?: return
        route.value = BreathStudioRoute.Player(
            config = SessionConfig.fromPractice(selectedPractice),
        )
    }

    fun openPresetPicker() {
        route.value = BreathStudioRoute.PresetPicker
    }

    fun createCustomPractice() {
        route.value = BreathStudioRoute.Builder(initialDefinition = null)
    }

    fun editSelectedPractice() {
        val authoredDefinition = appState.value.selectedEntry?.authoredDefinition ?: return
        route.value = BreathStudioRoute.Builder(
            initialDefinition = authoredDefinition,
        )
    }

    fun addPresetToLibrary(practice: BreathPractice) {
        viewModelScope.launch {
            val saved = appContainer.savedPracticeStore.upsertPractice(practice.toAuthoredDto())
            if (saved) {
                appContainer.userPreferencesRepository.setSelectedPracticeId(practice.safeId)
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
