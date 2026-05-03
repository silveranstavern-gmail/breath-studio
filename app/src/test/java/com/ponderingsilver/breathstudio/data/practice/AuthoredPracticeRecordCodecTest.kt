package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathRoute
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthoredPracticeRecordCodecTest {
    @Test
    fun codecRoundTripsNestedPracticeWithDelimiterText() {
        val dto = validDto(
            title = "Box | Breath",
            subtitle = "Calm\nFocus",
            description = "A practice with symbols | and newlines\ninside fields.",
            preferredVisualModeName = BreathingVisualMode.SquareTracer.name,
            blocks = listOf(
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                    title = "Stage | One",
                    target = AuthoredBlockTargetDto.durationMillis(90_000L),
                    cycle = AuthoredPracticeCycleDto(
                        steps = listOf(
                            AuthoredPracticeStepDto(
                                actionName = BreathAction.Inhale.name,
                                durationMillis = 5_000L,
                                label = "In | Left",
                                routeName = BreathRoute.Left.name,
                            ),
                            AuthoredPracticeStepDto(
                                actionName = BreathAction.Exhale.name,
                                durationMillis = 7_250L,
                                label = "Out\nRight",
                                routeName = BreathRoute.Right.name,
                            ),
                        ),
                    ),
                ),
                AuthoredPracticeBlockDto(
                    kind = AuthoredPracticeBlockDto.KindSequence,
                    title = "Hold\nBlock",
                    steps = listOf(
                        AuthoredPracticeStepDto(
                            actionName = BreathAction.HoldOut.name,
                            durationMillis = 60_000L,
                            label = "Hold | Still",
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
                            actionName = BreathAction.Inhale.name,
                            durationMillis = 4_000L,
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
