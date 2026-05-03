package com.ponderingsilver.breathstudio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ponderingsilver.breathstudio.data.practice.DataStoreSavedPracticeStore
import com.ponderingsilver.breathstudio.data.practice.DefaultPracticeRepository
import com.ponderingsilver.breathstudio.data.practice.PracticeRepository
import com.ponderingsilver.breathstudio.data.practice.SavedPracticeStore
import com.ponderingsilver.breathstudio.data.preferences.UserPreferencesRepository

class BreathStudioAppContainer(
    val userPreferencesRepository: UserPreferencesRepository,
    val savedPracticeStore: SavedPracticeStore,
    val practiceRepository: PracticeRepository,
)

@Composable
fun rememberBreathStudioAppContainer(
    practiceRepository: PracticeRepository? = null,
): BreathStudioAppContainer {
    val context = LocalContext.current
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

    return remember(userPreferencesRepository, savedPracticeStore, resolvedPracticeRepository) {
        BreathStudioAppContainer(
            userPreferencesRepository = userPreferencesRepository,
            savedPracticeStore = savedPracticeStore,
            practiceRepository = resolvedPracticeRepository,
        )
    }
}
