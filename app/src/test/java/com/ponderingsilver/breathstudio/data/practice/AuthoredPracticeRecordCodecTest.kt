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

        assertNull(AuthoredPracticeRecordCodec.decode("$encoded|extra"))
    }

    @Test
    fun codecDecodesLegacyStageRecord() {
        val legacyDto = AuthoredPracticeDto(
            id = "legacy",
            title = "Legacy Practice",
            stages = listOf(
                AuthoredPracticeStageDto(
                    title = "Legacy Stage",
                    target = AuthoredStageTargetDto.rounds(2),
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
        )
        val legacyRecord = encodeLegacyRecord(legacyDto)

        val decoded = AuthoredPracticeRecordCodec.decode(legacyRecord)

        assertNotNull(decoded)
        assertEquals("legacy", requireNotNull(decoded).id)
        assertEquals(1, decoded.stages.size)
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

    private fun encodeLegacyRecord(dto: AuthoredPracticeDto): String {
        val encode = java.util.Base64.getUrlEncoder().withoutPadding()
        fun token(value: String): String = encode.encodeToString(value.toByteArray(Charsets.UTF_8))

        val tokens = buildList {
            add("v1")
            add(token(dto.id))
            add(token(dto.title))
            add(token(dto.subtitle))
            add(token(dto.description))
            add(token(dto.category))
            add("~")
            add(dto.defaultDurationMinutes.toString())
            add(dto.stages.size.toString())
            dto.stages.forEach { stage ->
                add(token(stage.title))
                add(token(stage.target.kind))
                add(stage.target.value.toString())
                add(stage.cycle.steps.size.toString())
                stage.cycle.steps.forEach { step ->
                    add(token(step.actionName))
                    add((step.durationMillis / 1_000L).toString())
                    add(token(step.label))
                    add(step.routeName?.let(::token) ?: "~")
                }
            }
        }

        return tokens.joinToString("|")
    }
}
