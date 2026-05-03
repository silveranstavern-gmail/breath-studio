package com.ponderingsilver.breathstudio.data.practice

import java.util.Base64

internal object AuthoredPracticeRecordCodec {
    const val MaximumRecordLength: Int = 50_000

    private const val Version = "v2"
    private const val LegacyVersion = "v1"
    private const val NullToken = "~"
    private const val Separator = "|"

    fun encode(dto: AuthoredPracticeDto): String? {
        return runCatching {
            val tokens = buildList {
                add(Version)
                addEncoded(dto.id)
                addEncoded(dto.title)
                addEncoded(dto.subtitle)
                addEncoded(dto.description)
                addEncoded(dto.category)
                addNullableEncoded(dto.preferredVisualModeName)
                add(dto.defaultDurationMinutes.toString())
                add(dto.blocks.size.toString())
                dto.blocks.forEach { block ->
                    addEncoded(block.kind)
                    addEncoded(block.title)
                    add((block.target != null).toString())
                    if (block.target != null) {
                        addEncoded(block.target.kind)
                        add(block.target.value.toString())
                    }
                    add((block.cycle != null).toString())
                    if (block.cycle != null) {
                        add(block.cycle.steps.size.toString())
                        block.cycle.steps.forEach { step ->
                            addEncoded(step.actionName)
                            add(step.durationMillis.toString())
                            addEncoded(step.label)
                            addNullableEncoded(step.routeName)
                        }
                    }
                    add(block.steps.size.toString())
                    block.steps.forEach { step ->
                        addEncoded(step.actionName)
                        add(step.durationMillis.toString())
                        addEncoded(step.label)
                        addNullableEncoded(step.routeName)
                    }
                }
            }

            tokens.joinToString(Separator).takeIf { it.length <= MaximumRecordLength }
        }.getOrNull()
    }

    fun decode(record: String): AuthoredPracticeDto? {
        if (record.length > MaximumRecordLength) return null

        return runCatching {
            val cursor = Cursor(record.split(Separator))
            when (cursor.nextRaw()) {
                Version -> decodeV2(cursor)
                LegacyVersion -> decodeV1(cursor)
                else -> null
            }
        }.getOrNull()
    }

    private fun decodeV2(cursor: Cursor): AuthoredPracticeDto? {
        val id = cursor.nextDecoded()
        val title = cursor.nextDecoded()
        val subtitle = cursor.nextDecoded()
        val description = cursor.nextDecoded()
        val category = cursor.nextDecoded()
        val visualModeName = cursor.nextNullableDecoded()
        val defaultDurationMinutes = cursor.nextInt()
        val blockCount = cursor.nextCount(MaximumStoredBlocks)

        val blocks = buildList {
            repeat(blockCount) {
                val kind = cursor.nextDecoded()
                val blockTitle = cursor.nextDecoded()
                val hasTarget = cursor.nextBoolean()
                val target = if (hasTarget) {
                    AuthoredBlockTargetDto(
                        kind = cursor.nextDecoded(),
                        value = cursor.nextLong(),
                    )
                } else {
                    null
                }
                val hasCycle = cursor.nextBoolean()
                val cycle = if (hasCycle) {
                    val stepCount = cursor.nextCount(MaximumStoredStepsPerCycle)
                    AuthoredPracticeCycleDto(
                        steps = buildList {
                            repeat(stepCount) {
                                add(
                                    AuthoredPracticeStepDto(
                                        actionName = cursor.nextDecoded(),
                                        durationMillis = cursor.nextLong(),
                                        label = cursor.nextDecoded(),
                                        routeName = cursor.nextNullableDecoded(),
                                    ),
                                )
                            }
                        },
                    )
                } else {
                    null
                }
                val sequenceStepCount = cursor.nextCount(MaximumStoredStepsPerCycle)
                val steps = buildList {
                    repeat(sequenceStepCount) {
                        add(
                            AuthoredPracticeStepDto(
                                actionName = cursor.nextDecoded(),
                                durationMillis = cursor.nextLong(),
                                label = cursor.nextDecoded(),
                                routeName = cursor.nextNullableDecoded(),
                            ),
                        )
                    }
                }
                add(
                    AuthoredPracticeBlockDto(
                        kind = kind,
                        title = blockTitle,
                        target = target,
                        cycle = cycle,
                        steps = steps,
                    ),
                )
            }
        }

        if (cursor.hasRemaining) return null

        return AuthoredPracticeDto(
            id = id,
            title = title,
            subtitle = subtitle,
            description = description,
            category = category,
            blocks = blocks,
            preferredVisualModeName = visualModeName,
            defaultDurationMinutes = defaultDurationMinutes,
        )
    }

    private fun decodeV1(cursor: Cursor): AuthoredPracticeDto? {
        val id = cursor.nextDecoded()
        val title = cursor.nextDecoded()
        val subtitle = cursor.nextDecoded()
        val description = cursor.nextDecoded()
        val category = cursor.nextDecoded()
        val visualModeName = cursor.nextNullableDecoded()
        val defaultDurationMinutes = cursor.nextInt()
        val stageCount = cursor.nextCount(MaximumStoredStages)

        val stages = buildList {
            repeat(stageCount) {
                val stageTitle = cursor.nextDecoded()
                val targetKind = cursor.nextDecoded()
                val targetValue = cursor.nextInt()
                val stepCount = cursor.nextCount(MaximumStoredStepsPerCycle)
                val steps = buildList {
                    repeat(stepCount) {
                        add(
                            AuthoredPracticeStepDto(
                                actionName = cursor.nextDecoded(),
                                durationMillis = cursor.nextInt() * 1_000L,
                                label = cursor.nextDecoded(),
                                routeName = cursor.nextNullableDecoded(),
                            ),
                        )
                    }
                }
                add(
                    AuthoredPracticeStageDto(
                        title = stageTitle,
                        target = AuthoredStageTargetDto(
                            kind = targetKind,
                            value = targetValue,
                        ),
                        cycle = AuthoredPracticeCycleDto(steps),
                    ),
                )
            }
        }

        if (cursor.hasRemaining) return null

        return AuthoredPracticeDto(
            id = id,
            title = title,
            subtitle = subtitle,
            description = description,
            category = category,
            stages = stages,
            preferredVisualModeName = visualModeName,
            defaultDurationMinutes = defaultDurationMinutes,
        )
    }

    private fun MutableList<String>.addEncoded(value: String) {
        add(value.encodeToken())
    }

    private fun MutableList<String>.addNullableEncoded(value: String?) {
        add(value?.encodeToken() ?: NullToken)
    }

    private fun String.encodeToken(): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(toByteArray(Charsets.UTF_8))
    }

    private fun String.decodeToken(): String {
        return String(Base64.getUrlDecoder().decode(this), Charsets.UTF_8)
    }

    private class Cursor(
        private val tokens: List<String>,
    ) {
        private var index: Int = 0

        val hasRemaining: Boolean
            get() = index < tokens.size

        fun nextRaw(): String = tokens.getOrNull(index++) ?: error("Missing token")

        fun nextDecoded(): String = nextRaw().decodeToken()

        fun nextNullableDecoded(): String? {
            val raw = nextRaw()
            return if (raw == NullToken) null else raw.decodeToken()
        }

        fun nextInt(): Int = nextRaw().toInt()

        fun nextLong(): Long = nextRaw().toLong()

        fun nextBoolean(): Boolean = nextRaw().toBooleanStrict()

        fun nextCount(maximum: Int): Int = nextInt().coerceIn(0, maximum)
    }

    private const val MaximumStoredBlocks = 40
    private const val MaximumStoredStages = 40
    private const val MaximumStoredStepsPerCycle = 40
}
