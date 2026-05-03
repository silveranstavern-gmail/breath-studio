package com.ponderingsilver.breathstudio.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    context: Context,
) {
    private val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() },
        ),
        produceFile = { context.applicationContext.preferencesDataStoreFile(StoreName) },
    )

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException || throwable is CorruptionException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { storedPreferences ->
            UserPreferences(
                selectedPracticeId = sanitizePracticeId(storedPreferences[Keys.SelectedPracticeId]),
            )
        }

    suspend fun setSelectedPracticeId(practiceId: String) {
        safeEdit { preferences ->
            preferences[Keys.SelectedPracticeId] = sanitizePracticeId(practiceId)
        }
    }

    private suspend fun safeEdit(transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            dataStore.edit { preferences ->
                transform(preferences)
            }
        } catch (throwable: IOException) {
            // Keep the in-memory UI usable if storage is temporarily unavailable.
        } catch (throwable: CorruptionException) {
            // A corrupted file is replaced for reads; avoid crashing on a concurrent write.
        } catch (throwable: CancellationException) {
            throw throwable
        }
    }

    private object Keys {
        val SelectedPracticeId = stringPreferencesKey("selected_practice_id")
    }

    private companion object {
        const val StoreName = "user_preferences"

        fun sanitizePracticeId(value: String?): String {
            val sanitized = value
                ?.trim()
                ?.take(UserPreferences.MaximumPracticeIdLength)
                .orEmpty()
            return sanitized.ifBlank { UserPreferences.SelectedPracticeId }
        }
    }
}
