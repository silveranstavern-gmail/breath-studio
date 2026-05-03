package com.ponderingsilver.breathstudio

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.ponderingsilver.breathstudio.data.practice.DataStoreSavedPracticeStore
import com.ponderingsilver.breathstudio.data.practice.DefaultPracticeRepository
import com.ponderingsilver.breathstudio.data.practice.PracticeRepository
import com.ponderingsilver.breathstudio.data.practice.toAuthoredDefinitionOrNull
import com.ponderingsilver.breathstudio.data.practice.toAuthoredDto
import com.ponderingsilver.breathstudio.data.preferences.UserPreferences
import com.ponderingsilver.breathstudio.data.preferences.UserPreferencesRepository
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.BuiltInPractices
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.ui.builder.CustomPracticeBuilderScreen
import com.ponderingsilver.breathstudio.ui.home.PracticeHomeScreen
import com.ponderingsilver.breathstudio.ui.player.PracticePlayerScreen
import com.ponderingsilver.breathstudio.ui.theme.BreathStudioTheme
import kotlinx.coroutines.launch

data class CueSettings(
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
)

data class SessionConfig(
    val practice: BreathPractice,
    val durationMinutes: Int,
    val cues: CueSettings,
    val visualMode: BreathingVisualMode,
)

@Composable
fun BreathStudioApp(
    practiceRepository: PracticeRepository? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPreferencesRepository = remember(context) {
        UserPreferencesRepository(context.applicationContext)
    }
    val savedPracticeStore = remember(context) {
        DataStoreSavedPracticeStore(context.applicationContext)
    }
    val resolvedPracticeRepository = remember(practiceRepository, savedPracticeStore) {
        practiceRepository ?: DefaultPracticeRepository(
            savedPracticeDtos = savedPracticeStore.savedPracticeDtos,
        )
    }
    val userPreferences by userPreferencesRepository.preferences.collectAsState(
        initial = UserPreferences(),
    )
    val practices by resolvedPracticeRepository.practices.collectAsState(
        initial = BuiltInPractices.all,
    )
    val savedPracticeDtos by savedPracticeStore.savedPracticeDtos.collectAsState(initial = emptyList())
    var activeSession by remember { mutableStateOf<SessionConfig?>(null) }
    var builderInitialDefinition by remember { mutableStateOf<AuthoredPracticeDefinition?>(null) }

    val safePractices = remember(practices) {
        practices.ifEmpty { listOf(BuiltInPractices.BoxBreathing) }
    }
    val selectedPractice = remember(userPreferences.selectedPracticeId, safePractices) {
        safePractices.firstOrNull { it.safeId == userPreferences.selectedPracticeId }
            ?: safePractices.firstOrNull { it.safeId == BuiltInPractices.BoxBreathing.safeId }
            ?: safePractices.first()
    }
    var showingCustomPracticeBuilder by remember { mutableStateOf(false) }
    val selectedSavedDefinition = remember(savedPracticeDtos, selectedPractice) {
        savedPracticeDtos
            .firstOrNull { it.id.trim() == selectedPractice.safeId }
            ?.toAuthoredDefinitionOrNull()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = when {
                activeSession != null -> "player"
                showingCustomPracticeBuilder -> "builder"
                else -> "home"
            },
            label = "breath-studio-screen",
        ) { screen ->
            when (screen) {
                "home" -> {
                    PracticeHomeScreen(
                        practices = safePractices,
                        selectedPractice = selectedPractice,
                        onPracticeSelected = { practice ->
                            coroutineScope.launch {
                                userPreferencesRepository.setSelectedPracticeId(practice.safeId)
                            }
                        },
                        onStartSession = {
                            activeSession = SessionConfig(
                                practice = selectedPractice,
                                durationMinutes = selectedPractice.safeDefaultDurationMinutes,
                                cues = CueSettings(
                                    soundEnabled = UserPreferences.SoundEnabled,
                                    hapticsEnabled = UserPreferences.HapticsEnabled,
                                ),
                                visualMode = selectedPractice.preferredVisualMode,
                            )
                        },
                        onCreateCustomPractice = {
                            builderInitialDefinition = null
                            showingCustomPracticeBuilder = true
                        },
                        onEditSelectedPractice = {
                            builderInitialDefinition = selectedSavedDefinition
                            showingCustomPracticeBuilder = true
                        },
                        canEditSelectedPractice = selectedSavedDefinition != null,
                    )
                }
                "builder" -> {
                    CustomPracticeBuilderScreen(
                        initialDefinition = builderInitialDefinition,
                        onCancel = { showingCustomPracticeBuilder = false },
                        onSave = { definition ->
                            val saved = savedPracticeStore.upsertPractice(definition.toAuthoredDto())
                            if (saved) {
                                userPreferencesRepository.setSelectedPracticeId(definition.safeId)
                            }
                            saved
                        },
                    )
                }
                else -> {
                    PracticePlayerScreen(
                        config = requireNotNull(activeSession),
                        onBack = { activeSession = null },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BreathStudioPreview() {
    BreathStudioTheme(darkTheme = true) {
        BreathStudioApp()
    }
}
