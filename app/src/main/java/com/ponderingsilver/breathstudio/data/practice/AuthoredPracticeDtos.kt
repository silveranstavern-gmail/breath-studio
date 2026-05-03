package com.ponderingsilver.breathstudio.data.practice

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.domain.model.BreathRoute
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.PracticeCycle
import com.ponderingsilver.breathstudio.domain.model.PracticeStage
import com.ponderingsilver.breathstudio.domain.model.PracticeStageTarget
import com.ponderingsilver.breathstudio.domain.model.PracticeStep
import com.ponderingsilver.breathstudio.domain.model.toAuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.toDomainPracticeOrNull
import kotlinx.serialization.Serializable

@Serializable
data class AuthoredPracticeDto(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val category: String = "",
    val blocks: List<AuthoredPracticeBlockDto> = emptyList(),
    val stages: List<AuthoredPracticeStageDto> = emptyList(),
    val preferredVisualModeName: String? = null,
    val defaultDurationMinutes: Int = DefaultDurationMinutes,
) {
    companion object {
        const val DefaultDurationMinutes: Int = 5
    }
}

@Serializable
data class AuthoredPracticeStageDto(
    val title: String = "",
    val target: AuthoredStageTargetDto = AuthoredStageTargetDto.rounds(1),
    val cycle: AuthoredPracticeCycleDto = AuthoredPracticeCycleDto(),
)

@Serializable
data class AuthoredPracticeCycleDto(
    val steps: List<AuthoredPracticeStepDto> = emptyList(),
)

@Serializable
data class AuthoredPracticeBlockDto(
    val kind: String,
    val title: String = "",
    val target: AuthoredBlockTargetDto? = null,
    val cycle: AuthoredPracticeCycleDto? = null,
    val steps: List<AuthoredPracticeStepDto> = emptyList(),
) {
    companion object {
        const val KindRepeatingCycle = "repeating_cycle"
        const val KindSequence = "sequence"
    }
}

@Serializable
data class AuthoredPracticeStepDto(
    val actionName: String,
    val durationMillis: Long,
    val label: String = "",
    val routeName: String? = null,
)

@Serializable
data class AuthoredBlockTargetDto(
    val kind: String,
    val value: Long,
) {
    companion object {
        const val KindRepetitions = "repetitions"
        const val KindDurationMillis = "duration_millis"

        fun repetitions(value: Int): AuthoredBlockTargetDto = AuthoredBlockTargetDto(
            kind = KindRepetitions,
            value = value.toLong(),
        )

        fun durationMillis(value: Long): AuthoredBlockTargetDto = AuthoredBlockTargetDto(
            kind = KindDurationMillis,
            value = value,
        )
    }
}

@Serializable
data class AuthoredStageTargetDto(
    val kind: String,
    val value: Int,
) {
    companion object {
        const val KindRounds = "rounds"
        const val KindDurationSeconds = "duration_seconds"

        fun rounds(value: Int): AuthoredStageTargetDto = AuthoredStageTargetDto(
            kind = KindRounds,
            value = value,
        )

        fun durationSeconds(value: Int): AuthoredStageTargetDto = AuthoredStageTargetDto(
            kind = KindDurationSeconds,
            value = value,
        )
    }
}

fun AuthoredPracticeDto.toDomainOrNull(): BreathPractice? {
    return toAuthoredDefinitionOrNull()?.toDomainPracticeOrNull()
}

fun AuthoredPracticeDto.toAuthoredDefinitionOrNull(): AuthoredPracticeDefinition? {
    val sanitizedId = id.safeRequiredText(MaximumIdLength) ?: return null
    val domainBlocks = when {
        blocks.isNotEmpty() -> blocks.take(MaximumBlocks).mapNotNull { it.toDomainOrNull() }
        else -> stages.take(MaximumStages).mapNotNull { it.toLegacyBlockOrNull() }
    }
    if (domainBlocks.isEmpty()) return null

    return runCatching {
        AuthoredPracticeDefinition(
            id = sanitizedId,
            title = title.safeText("Untitled Practice", MaximumTitleLength),
            subtitle = subtitle.safeText("Custom breathing", MaximumSubtitleLength),
            description = description.safeText("Follow each cue at a comfortable pace.", MaximumDescriptionLength),
            category = category.safeText("Saved", MaximumCategoryLength),
            blocks = domainBlocks,
            preferredVisualMode = parseVisualMode(preferredVisualModeName),
            defaultDurationMinutes = defaultDurationMinutes.coerceIn(MinimumDurationMinutes, MaximumDurationMinutes),
        )
    }.getOrNull()
}

fun BreathPractice.toAuthoredDto(): AuthoredPracticeDto = AuthoredPracticeDto(
    blocks = toAuthoredPracticeDefinition().toAuthoredDto().blocks,
    id = safeId,
    title = safeTitle,
    subtitle = safeSubtitle,
    description = safeDescription,
    category = safeCategory,
    stages = stages.map { stage ->
        AuthoredPracticeStageDto(
            title = stage.safeTitle,
            target = when (val target = stage.target) {
                is PracticeStageTarget.Rounds -> AuthoredStageTargetDto.rounds(target.count)
                is PracticeStageTarget.DurationSeconds -> AuthoredStageTargetDto.durationSeconds(target.seconds)
            },
            cycle = AuthoredPracticeCycleDto(
                steps = stage.cycle.steps.map { step ->
                    AuthoredPracticeStepDto(
                        actionName = step.action.name,
                        durationMillis = step.durationMillis,
                        label = step.safeLabel,
                        routeName = step.route.name,
                    )
                },
            ),
        )
    },
    preferredVisualModeName = preferredVisualMode.name,
    defaultDurationMinutes = safeDefaultDurationMinutes,
)

fun AuthoredPracticeDefinition.toAuthoredDto(): AuthoredPracticeDto = AuthoredPracticeDto(
    id = safeId,
    title = safeTitle,
    subtitle = safeSubtitle,
    description = safeDescription,
    category = safeCategory,
    blocks = blocks.map { block ->
        when (block) {
            is AuthoredPracticeBlock.RepeatingCycle -> AuthoredPracticeBlockDto(
                kind = AuthoredPracticeBlockDto.KindRepeatingCycle,
                title = block.safeTitle,
                target = when (val target = block.target) {
                    is AuthoredBlockTarget.Repetitions -> AuthoredBlockTargetDto.repetitions(target.count)
                    is AuthoredBlockTarget.DurationMillis -> AuthoredBlockTargetDto.durationMillis(target.durationMillis)
                },
                cycle = AuthoredPracticeCycleDto(
                    steps = block.cycle.steps.map { step -> step.toDto() },
                ),
            )
            is AuthoredPracticeBlock.Sequence -> AuthoredPracticeBlockDto(
                kind = AuthoredPracticeBlockDto.KindSequence,
                title = block.safeTitle,
                steps = block.steps.map { step -> step.toDto() },
            )
        }
    },
    preferredVisualModeName = preferredVisualMode.name,
    defaultDurationMinutes = safeDefaultDurationMinutes,
)

private fun AuthoredPracticeStageDto.toDomainOrNull(): PracticeStage? {
    val domainSteps = cycle.steps.take(MaximumStepsPerCycle).mapNotNull { it.toDomainOrNull() }
    if (domainSteps.isEmpty()) return null

    return runCatching {
        PracticeStage(
            title = title.safeText("Practice stage", MaximumTitleLength),
            target = target.toDomainTarget(),
            cycle = PracticeCycle(domainSteps),
        )
    }.getOrNull()
}

private fun AuthoredPracticeStageDto.toLegacyBlockOrNull(): AuthoredPracticeBlock? {
    val cycleSteps = cycle.steps.take(MaximumStepsPerCycle).mapNotNull { it.toAuthoredStepOrNull() }
    if (cycleSteps.isEmpty()) return null

    return AuthoredPracticeBlock.RepeatingCycle(
        title = title.safeText("Practice stage", MaximumTitleLength),
        target = target.toAuthoredTarget(),
        cycle = AuthoredPracticeCycle(cycleSteps),
    )
}

private fun AuthoredPracticeBlockDto.toDomainOrNull(): AuthoredPracticeBlock? {
    return when (kind.trim()) {
        AuthoredPracticeBlockDto.KindSequence -> {
            val domainSteps = steps.take(MaximumStepsPerCycle).mapNotNull { it.toAuthoredStepOrNull() }
            if (domainSteps.isEmpty()) null else AuthoredPracticeBlock.Sequence(
                title = title.safeText("Practice block", MaximumTitleLength),
                steps = domainSteps,
            )
        }
        else -> {
            val domainSteps = cycle?.steps.orEmpty().take(MaximumStepsPerCycle).mapNotNull { it.toAuthoredStepOrNull() }
            if (domainSteps.isEmpty()) null else AuthoredPracticeBlock.RepeatingCycle(
                title = title.safeText("Practice block", MaximumTitleLength),
                cycle = AuthoredPracticeCycle(domainSteps),
                target = target?.toDomainTarget() ?: AuthoredBlockTarget.Repetitions(1),
            )
        }
    }
}

private fun AuthoredPracticeStepDto.toDomainOrNull(): PracticeStep? {
    val action = BreathAction.entries.firstOrNull { it.name == actionName.trim() } ?: return null
    return PracticeStep(
        action = action,
        durationSeconds = ceilDiv(durationMillis.coerceIn(MinimumStepDurationMillis, MaximumStepDurationMillis), 1_000L)
            .coerceAtMost(MaximumStepDurationSeconds.toLong())
            .toInt(),
        label = label.safeText(action.label, MaximumStepLabelLength),
        route = parseRoute(routeName),
    )
}

private fun AuthoredPracticeStepDto.toAuthoredStepOrNull(): AuthoredPracticeStep? {
    val action = BreathAction.entries.firstOrNull { it.name == actionName.trim() } ?: return null
    return AuthoredPracticeStep(
        action = action,
        durationMillis = durationMillis.coerceIn(MinimumStepDurationMillis, MaximumStepDurationMillis),
        label = label.safeText(action.label, MaximumStepLabelLength),
        route = parseRoute(routeName),
    )
}

private fun AuthoredPracticeStep.toDto(): AuthoredPracticeStepDto = AuthoredPracticeStepDto(
    actionName = action.name,
    durationMillis = safeDurationMillis,
    label = safeLabel,
    routeName = route.name,
)

private fun AuthoredBlockTargetDto.toDomainTarget(): AuthoredBlockTarget {
    return when (kind.trim()) {
        AuthoredBlockTargetDto.KindDurationMillis -> AuthoredBlockTarget.DurationMillis(
            value.coerceIn(MinimumBlockDurationMillis, MaximumBlockDurationMillis),
        )
        else -> AuthoredBlockTarget.Repetitions(
            value.coerceIn(MinimumBlockRepetitions.toLong(), MaximumBlockRepetitions.toLong()).toInt(),
        )
    }
}

private fun AuthoredStageTargetDto.toAuthoredTarget(): AuthoredBlockTarget {
    return when (kind.trim()) {
        AuthoredStageTargetDto.KindDurationSeconds -> AuthoredBlockTarget.DurationMillis(
            value.coerceIn(MinimumStageDurationSeconds, MaximumStageDurationSeconds) * 1_000L,
        )
        else -> AuthoredBlockTarget.Repetitions(
            value.coerceIn(MinimumStageRounds, MaximumStageRounds),
        )
    }
}

private fun AuthoredStageTargetDto.toDomainTarget(): PracticeStageTarget {
    return when (kind.trim()) {
        AuthoredStageTargetDto.KindDurationSeconds -> PracticeStageTarget.DurationSeconds(
            value.coerceIn(MinimumStageDurationSeconds, MaximumStageDurationSeconds),
        )
        else -> PracticeStageTarget.Rounds(
            value.coerceIn(MinimumStageRounds, MaximumStageRounds),
        )
    }
}

private fun parseVisualMode(value: String?): BreathingVisualMode {
    return BreathingVisualMode.entries.firstOrNull { it.name == value?.trim() }
        ?: BreathingVisualMode.Circle
}

private fun parseRoute(value: String?): BreathRoute {
    return BreathRoute.entries.firstOrNull { it.name == value?.trim() }
        ?: BreathRoute.Both
}

private fun String.safeText(
    fallback: String,
    maxLength: Int,
): String {
    val sanitized = trim().take(maxLength)
    return sanitized.ifBlank { fallback }
}

private fun String.safeRequiredText(maxLength: Int): String? {
    val sanitized = trim().take(maxLength)
    return sanitized.ifBlank { null }
}

private const val MinimumDurationMinutes = 1
private const val MaximumDurationMinutes = 240
private const val MinimumStepDurationMillis = 100L
private const val MaximumStepDurationMillis = 3_600_000L
private const val MaximumStepDurationSeconds = 3_600
private const val MinimumStageRounds = 1
private const val MaximumStageRounds = 1_000
private const val MinimumStageDurationSeconds = 1
private const val MaximumStageDurationSeconds = 24 * 60 * 60
private const val MinimumBlockRepetitions = 1
private const val MaximumBlockRepetitions = 10_000
private const val MinimumBlockDurationMillis = 100L
private const val MaximumBlockDurationMillis = 24L * 60L * 60L * 1_000L
private const val MaximumIdLength = 128
private const val MaximumTitleLength = 80
private const val MaximumSubtitleLength = 120
private const val MaximumDescriptionLength = 600
private const val MaximumCategoryLength = 40
private const val MaximumStepLabelLength = 40
private const val MaximumBlocks = 40
private const val MaximumStages = 40
private const val MaximumStepsPerCycle = 40

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
