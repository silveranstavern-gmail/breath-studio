package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthoredPracticeRecordCodecTest {
    @Test
    fun codecRoundTripsSimplePractice() {
        val dto = validDto(
            title = "Box Breath",
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    title = "Main",
                    target = AuthoredBlockTargetDto.durationMillis(300_000L),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                durationMillis = 4_000L,
                                label = "Inhale",
                                colorHex = "#7ED9C8",
                            ),
                            AuthoredPracticeStepDto(
                                durationMillis = 4_000L,
                                label = "Exhale",
                                colorHex = "#9BC1FF",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val encoded = AuthoredPracticeRecordCodec.encode(dto)
        val decoded = encoded?.let(AuthoredPracticeRecordCodec::decode)

        assertNotNull(encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun codecDropsMalformedOrOversizedRecords() {
        assertNull(AuthoredPracticeRecordCodec.decode("not-a-valid-record"))
        assertNull(AuthoredPracticeRecordCodec.decode("x".repeat(AuthoredPracticeRecordCodec.MaximumRecordLength + 1)))
    }

    @Test
    fun codecDropsRecordsWithTrailingGarbage() {
        val encoded = requireNotNull(AuthoredPracticeRecordCodec.encode(validDto()))

        assertNull(AuthoredPracticeRecordCodec.decode("$encoded extra"))
    }

    private fun validDto(
        id: String = "custom",
        title: String = "Custom Practice",
        subtitle: String = "Saved breath",
        description: String = "Follow each cue.",
        category: String = "Saved",
        preferredVisualModeName: String? = null,
        blocks: List<AuthoredPracticeBlockDto> = listOf(
            AuthoredPracticeBlockDto(
                kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                target = AuthoredBlockTargetDto.repetitions(1),
                cycle = AuthoredPracticeCycleDto(
                    steps = listOf(
                        AuthoredPracticeStepDto(
                            durationMillis = 4_000L,
                            label = "Inhale",
                        ),
                    ),
                ),
            ),
        ),
    ): AuthoredPracticeDto = AuthoredPracticeDto(
        id = id,
        title = title,
        subtitle = subtitle,
        description = description,
        category = category,
        blocks = blocks,
        preferredVisualModeName = preferredVisualModeName,
    )

}

