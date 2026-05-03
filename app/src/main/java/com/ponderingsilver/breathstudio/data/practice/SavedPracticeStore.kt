package com.ponderingsilver.breathstudio.data.practice

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SavedPracticeStore {
    val savedPracticeDtos: Flow<List<AuthoredPracticeDto>>

    suspend fun upsertPractice(dto: AuthoredPracticeDto): Boolean

    suspend fun deletePractice(id: String): Boolean
}

class DataStoreSavedPracticeStore(
    context: Context,
) : SavedPracticeStore {
    private val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() },
        ),
        produceFile = { context.applicationContext.preferencesDataStoreFile(StoreName) },
    )

    override val savedPracticeDtos: Flow<List<AuthoredPracticeDto>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException || throwable is CorruptionException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            decodeRecords(preferences[Keys.SavedPracticeRecords].orEmpty())
        }

    override suspend fun upsertPractice(dto: AuthoredPracticeDto): Boolean {
        val canonicalDto = dto.toAuthoredDefinitionOrNull()?.toAuthoredDto() ?: return false
        val record = AuthoredPracticeRecordCodec.encode(canonicalDto) ?: return false
        val canonicalId = canonicalDto.id.trim()
        if (canonicalId.isBlank()) return false

        return safeEdit { records ->
            val existingDtos = decodeRecords(records)
                .filterNot { existing -> existing.id == canonicalId }
                .take(MaximumSavedPractices - 1)
            val nextRecords = (existingDtos.mapNotNull(AuthoredPracticeRecordCodec::encode) + record)
                .filter { it.length <= AuthoredPracticeRecordCodec.MaximumRecordLength }
                .toSet()
            nextRecords
        }
    }

    override suspend fun deletePractice(id: String): Boolean {
        val canonicalId = id.trim().take(MaximumPracticeIdLength)
        if (canonicalId.isBlank()) return false

        return safeEdit { records ->
            decodeRecords(records)
                .filterNot { existing -> existing.id == canonicalId }
                .mapNotNull(AuthoredPracticeRecordCodec::encode)
                .toSet()
        }
    }

    private suspend fun safeEdit(transform: (Set<String>) -> Set<String>): Boolean {
        return try {
            dataStore.edit { preferences ->
                val current = preferences[Keys.SavedPracticeRecords].orEmpty()
                preferences[Keys.SavedPracticeRecords] = transform(current)
            }
            true
        } catch (throwable: IOException) {
            false
        } catch (throwable: CorruptionException) {
            false
        } catch (throwable: CancellationException) {
            throw throwable
        }
    }

    private fun decodeRecords(records: Set<String>): List<AuthoredPracticeDto> {
        return records
            .asSequence()
            .take(MaximumStoredRecords)
            .mapNotNull(AuthoredPracticeRecordCodec::decode)
            .mapNotNull { dto -> dto.toAuthoredDefinitionOrNull()?.toAuthoredDto() }
            .distinctBy { dto -> dto.id }
            .sortedWith(compareBy<AuthoredPracticeDto> { it.title.lowercase() }.thenBy { it.id })
            .toList()
    }

    private object Keys {
        val SavedPracticeRecords = stringSetPreferencesKey("saved_practice_records")
    }

    private companion object {
        const val StoreName = "saved_practices"
        const val MaximumSavedPractices = 100
        const val MaximumStoredRecords = 200
        const val MaximumPracticeIdLength = 128
    }
}
