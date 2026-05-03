package com.ponderingsilver.breathstudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ponderingsilver.breathstudio.data.practice.PracticeLibraryEntry
import com.ponderingsilver.breathstudio.data.practice.PracticeSource
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
    val entries: List<PracticeLibraryEntry> = DefaultEntries,
    val selectedEntry: PracticeLibraryEntry = DefaultEntries.first(),
) {
    val practices: List<BreathPractice>
        get() = entries.map { entry -> entry.practice }

    companion object {
        val DefaultEntries: List<PracticeLibraryEntry> = BuiltInPractices.all.map { practice ->
            PracticeLibraryEntry(
                practice = practice,
                source = PracticeSource.BuiltIn,
            )
        }.ifEmpty {
            listOf(
                PracticeLibraryEntry(
                    practice = BuiltInPractices.BoxBreathing,
                    source = PracticeSource.BuiltIn,
                ),
            )
        }
    }
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
        val safeEntries = libraryEntries.ifEmpty { BreathStudioAppState.DefaultEntries }
        val selectedEntry = safeEntries.selectedEntryFor(userPreferences)
        BreathStudioAppState(
            route = currentRoute,
            entries = safeEntries,
            selectedEntry = selectedEntry,
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
        route.value = BreathStudioRoute.Player(
            config = SessionConfig.fromPractice(appState.value.selectedEntry.practice),
        )
    }

    fun createCustomPractice() {
        route.value = BreathStudioRoute.Builder(initialDefinition = null)
    }

    fun editSelectedPractice() {
        route.value = BreathStudioRoute.Builder(
            initialDefinition = appState.value.selectedEntry.authoredDefinition,
        )
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
    ): PracticeLibraryEntry {
        return firstOrNull { entry -> entry.practice.safeId == userPreferences.selectedPracticeId }
            ?: firstOrNull { entry -> entry.practice.safeId == BuiltInPractices.BoxBreathing.safeId }
            ?: first()
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
