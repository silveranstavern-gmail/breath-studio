package com.ponderingsilver.breathstudio

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ponderingsilver.breathstudio.data.practice.PracticeRepository
import com.ponderingsilver.breathstudio.ui.builder.CustomPracticeBuilderScreen
import com.ponderingsilver.breathstudio.ui.home.PresetPickerScreen
import com.ponderingsilver.breathstudio.ui.home.PracticeHomeScreen
import com.ponderingsilver.breathstudio.ui.player.PracticePlayerScreen
import com.ponderingsilver.breathstudio.ui.support.SupportScreen
import com.ponderingsilver.breathstudio.ui.theme.BreathStudioTheme

@Composable
fun BreathStudioApp(
    practiceRepository: PracticeRepository? = null,
) {
    val appContainer = rememberBreathStudioAppContainer(practiceRepository)
    val appViewModel: BreathStudioAppViewModel = viewModel(
        factory = BreathStudioAppViewModelFactory(appContainer),
    )
    val appState by appViewModel.appState.collectAsState()
    val isOnHomeRoute = appState.route == BreathStudioRoute.Home

    BackHandler(enabled = !isOnHomeRoute) {
        appViewModel.goHome()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = appState.route,
            label = "breath-studio-screen",
        ) { currentRoute ->
            when (currentRoute) {
                BreathStudioRoute.Home -> {
                    PracticeHomeScreen(
                        entries = appState.entries,
                        selectedPractice = appState.selectedEntry?.practice,
                        onPracticeSelected = appViewModel::selectPractice,
                        onStartSession = appViewModel::startPractice,
                        onAddPreset = appViewModel::openPresetPicker,
                        onCreateCustomPractice = appViewModel::createCustomPractice,
                        onCopyPractice = appViewModel::copyPractice,
                        onEditPractice = appViewModel::editPractice,
                        onDeleteSelectedPractice = appViewModel::deleteSelectedPractice,
                        onOpenSupport = appViewModel::openSupport,
                        canManageSelectedPractice = appState.selectedEntry?.canEdit == true,
                    )
                }
                BreathStudioRoute.Support -> {
                    SupportScreen(
                        onBack = appViewModel::goHome,
                    )
                }
                BreathStudioRoute.PresetPicker -> {
                    PresetPickerScreen(
                        presets = appState.availablePresets,
                        onAddPreset = appViewModel::addPresetToLibrary,
                        onBack = appViewModel::goHome,
                    )
                }
                is BreathStudioRoute.Builder -> {
                    CustomPracticeBuilderScreen(
                        initialDefinition = currentRoute.initialDefinition,
                        onCancel = appViewModel::goHome,
                        onSave = appViewModel::savePractice,
                    )
                }
                is BreathStudioRoute.Player -> {
                    PracticePlayerScreen(
                        config = currentRoute.config,
                        onBack = appViewModel::goHome,
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
