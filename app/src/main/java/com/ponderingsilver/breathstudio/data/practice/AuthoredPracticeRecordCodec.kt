package com.ponderingsilver.breathstudio.data.practice

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object AuthoredPracticeRecordCodec {
    const val MaximumRecordLength: Int = 50_000

    private val JsonCodec = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(dto: AuthoredPracticeDto): String? {
        return try {
            JsonCodec.encodeToString(dto)
                .takeIf { record -> record.length <= MaximumRecordLength }
        } catch (exception: SerializationException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    fun decode(record: String): AuthoredPracticeDto? {
        if (record.length > MaximumRecordLength) return null

        return try {
            JsonCodec.decodeFromString<AuthoredPracticeDto>(record)
        } catch (exception: SerializationException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }
    }
}
