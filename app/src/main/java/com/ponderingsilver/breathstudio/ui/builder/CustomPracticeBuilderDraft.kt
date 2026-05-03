package com.ponderingsilver.breathstudio.ui.builder

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathRoute
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToLong

enum class BuilderTargetMode {
    DurationMinutes,
    Repetitions,
}

data class EditablePracticeStep(
    val id: String,
    val action: BreathAction,
    val durationInput: String,
)

data class EditablePracticeBlock(
    val id: String,
    val title: String,
    val targetMode: BuilderTargetMode,
    val targetValueInput: String,
    val steps: List<EditablePracticeStep>,
)

data class CustomPracticeBuilderDraft(
    val practiceId: String? = null,
    val title: String = "",
    val visualMode: BreathingVisualMode = BreathingVisualMode.Circle,
    val blocks: List<EditablePracticeBlock> = listOf(defaultEditableBlock()),
    val selectedBlockId: String = blocks.first().id,
)

val CustomPracticeBuilderDraft.hasExistingPractice: Boolean
    get() = !practiceId.isNullOrBlank()

val CustomPracticeBuilderDraft.selectedBlock: EditablePracticeBlock
    get() = blocks.firstOrNull { it.id == selectedBlockId } ?: blocks.first()

fun defaultEditableSteps(): List<EditablePracticeStep> = listOf(
    EditablePracticeStep(
        id = "step-inhale",
        action = BreathAction.Inhale,
        durationInput = "4",
    ),
    EditablePracticeStep(
        id = "step-hold-in",
        action = BreathAction.HoldIn,
        durationInput = "4",
    ),
    EditablePracticeStep(
        id = "step-exhale",
        action = BreathAction.Exhale,
        durationInput = "4",
    ),
    EditablePracticeStep(
        id = "step-hold-out",
        action = BreathAction.HoldOut,
        durationInput = "4",
    ),
)

fun defaultEditableBlock(): EditablePracticeBlock = EditablePracticeBlock(
    id = "block-main",
    title = "Main cycle",
    targetMode = BuilderTargetMode.DurationMinutes,
    targetValueInput = "5",
    steps = defaultEditableSteps(),
)

fun nextSuggestedAction(previous: BreathAction?): BreathAction = when (previous) {
    BreathAction.Inhale -> BreathAction.Exhale
    BreathAction.Exhale -> BreathAction.Inhale
    BreathAction.HoldIn -> BreathAction.Exhale
    BreathAction.HoldOut -> BreathAction.Inhale
    BreathAction.Rest, null -> BreathAction.Inhale
}

fun AuthoredPracticeDefinition.toBuilderDraftOrNull(): CustomPracticeBuilderDraft? {
    if (blocks.isEmpty()) return null
    val editableBlocks = blocks.mapIndexed { index, block ->
        val repeatingBlock = block as? AuthoredPracticeBlock.RepeatingCycle ?: return null
        EditablePracticeBlock(
            id = "${safeId}-block-$index",
            title = repeatingBlock.safeTitle,
            targetMode = when (repeatingBlock.target) {
                is AuthoredBlockTarget.DurationMillis -> BuilderTargetMode.DurationMinutes
                is AuthoredBlockTarget.Repetitions -> BuilderTargetMode.Repetitions
            },
            targetValueInput = when (val target = repeatingBlock.target) {
                is AuthoredBlockTarget.DurationMillis -> ceil(target.durationMillis / 60_000.0).toInt().toString()
                is AuthoredBlockTarget.Repetitions -> target.count.toString()
            },
            steps = repeatingBlock.cycle.steps.mapIndexed { stepIndex, step ->
                EditablePracticeStep(
                    id = "${safeId}-block-$index-step-$stepIndex",
                    action = step.action,
                    durationInput = formatSeconds(step.safeDurationMillis),
                )
            },
        )
    }
    return CustomPracticeBuilderDraft(
        practiceId = safeId,
        title = safeTitle,
        visualMode = preferredVisualMode,
        blocks = editableBlocks,
        selectedBlockId = editableBlocks.first().id,
    )
}

fun CustomPracticeBuilderDraft.toAuthoredPracticeDefinitionOrNull(
    idProvider: () -> String,
): AuthoredPracticeDefinition? {
    val authoredBlocks = blocks.mapNotNull { block -> block.toAuthoredBlockOrNull() }
    if (authoredBlocks.size != blocks.size || authoredBlocks.isEmpty()) return null

    val practiceTitle = title.trim().ifBlank { "Custom Practice" }
    return AuthoredPracticeDefinition(
        id = practiceId?.trim().takeUnless { it.isNullOrBlank() } ?: idProvider().trim().ifBlank { "custom-practice" },
        title = practiceTitle,
        subtitle = draftSubtitle(authoredBlocks),
        description = draftDescription(authoredBlocks),
        category = "Custom",
        blocks = authoredBlocks,
        preferredVisualMode = visualMode,
        defaultDurationMinutes = suggestedDefaultDurationMinutes(authoredBlocks),
    )
}

fun EditablePracticeBlock.summaryLabel(): String {
    val cadence = steps.mapNotNull { step ->
        parseDurationMillis(step.durationInput)?.let { durationMillis ->
            "${step.action.label} ${formatSeconds(durationMillis)}s"
        }
    }.joinToString(" / ")
    if (cadence.isBlank()) return "Enter valid step durations."

    val targetSummary = when (targetMode) {
        BuilderTargetMode.DurationMinutes -> targetValueInput.trim().toIntOrNull()?.let { "$it min" }
        BuilderTargetMode.Repetitions -> targetValueInput.trim().toIntOrNull()?.let { "$it rounds" }
    } ?: "set target"
    return "$cadence • $targetSummary"
}

fun EditablePracticeBlock.cycleDurationMillisOrNull(): Long? {
    val durations = steps.mapNotNull { step -> parseDurationMillis(step.durationInput) }
    return if (durations.size == steps.size && durations.isNotEmpty()) durations.sum() else null
}

fun EditablePracticeBlock.estimatedBlockDurationMillisOrNull(): Long? {
    val cycleDurationMillis = cycleDurationMillisOrNull() ?: return null
    return when (targetMode) {
        BuilderTargetMode.DurationMinutes -> targetValueInput.trim().toIntOrNull()?.coerceAtLeast(1)?.times(60_000L)
        BuilderTargetMode.Repetitions -> targetValueInput.trim().toIntOrNull()?.coerceAtLeast(1)?.times(cycleDurationMillis)
    }
}

fun CustomPracticeBuilderDraft.estimatedTotalDurationMillisOrNull(): Long? {
    val durations = blocks.mapNotNull { it.estimatedBlockDurationMillisOrNull() }
    return if (durations.size == blocks.size && durations.isNotEmpty()) durations.sum() else null
}

fun CustomPracticeBuilderDraft.summaryLabel(): String {
    val blockCount = blocks.size
    val totalLabel = estimatedTotalDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "invalid session"
    return "$blockCount block${if (blockCount == 1) "" else "s"} • $totalLabel"
}

private fun EditablePracticeBlock.toAuthoredBlockOrNull(): AuthoredPracticeBlock.RepeatingCycle? {
    val authoredSteps = steps.mapNotNull { step ->
        val durationMillis = parseDurationMillis(step.durationInput) ?: return null
        AuthoredPracticeStep(
            action = step.action,
            durationMillis = durationMillis,
            label = step.action.label,
            route = BreathRoute.Both,
        )
    }
    if (authoredSteps.isEmpty()) return null

    val blockTarget = when (targetMode) {
        BuilderTargetMode.DurationMinutes -> {
            val durationMinutes = targetValueInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: return null
            AuthoredBlockTarget.DurationMillis(durationMinutes * 60_000L)
        }
        BuilderTargetMode.Repetitions -> {
            val repetitions = targetValueInput.trim().toIntOrNull()?.coerceAtLeast(1) ?: return null
            AuthoredBlockTarget.Repetitions(repetitions)
        }
    }

    return AuthoredPracticeBlock.RepeatingCycle(
        title = title.trim().ifBlank { "Practice block" },
        cycle = AuthoredPracticeCycle(authoredSteps),
        target = blockTarget,
    )
}

private fun draftSubtitle(blocks: List<AuthoredPracticeBlock.RepeatingCycle>): String {
    if (blocks.size == 1) {
        return blocks.first().cycle.steps.joinToString(" / ") { step ->
            "${step.action.label} ${formatSeconds(step.safeDurationMillis)}s"
        }
    }
    return "${blocks.size} block routine"
}

private fun draftDescription(blocks: List<AuthoredPracticeBlock.RepeatingCycle>): String {
    return blocks.joinToString(separator = " Then ") { block ->
        val cadence = block.cycle.steps.joinToString(", ") { step ->
            "${step.action.label.lowercase()} ${formatSeconds(step.safeDurationMillis)} seconds"
        }
        val targetSummary = when (val target = block.target) {
            is AuthoredBlockTarget.DurationMillis -> "for ${target.durationMillis / 60_000L} minutes"
            is AuthoredBlockTarget.Repetitions -> "for ${target.count} rounds"
        }
        "${block.safeTitle}: $cadence $targetSummary"
    }.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
    }
}

private fun suggestedDefaultDurationMinutes(blocks: List<AuthoredPracticeBlock.RepeatingCycle>): Int {
    val totalDurationMillis = blocks.sumOf { block ->
        when (val target = block.target) {
            is AuthoredBlockTarget.DurationMillis -> target.durationMillis
            is AuthoredBlockTarget.Repetitions -> block.cycle.durationMillis * target.count.coerceAtLeast(1)
        }
    }
    return ceil(totalDurationMillis / 60_000.0).toInt().coerceAtLeast(1)
}

fun newEditableBlockFromTemplate(block: EditablePracticeBlock? = null): EditablePracticeBlock {
    if (block == null) {
        return defaultEditableBlock().copy(
            id = UUID.randomUUID().toString(),
            steps = defaultEditableSteps().map { it.copy(id = UUID.randomUUID().toString()) },
        )
    }
    return block.copy(
        id = UUID.randomUUID().toString(),
        steps = block.steps.map { it.copy(id = UUID.randomUUID().toString()) },
    )
}

private fun parseDurationMillis(input: String): Long? {
    val seconds = input.trim().toDoubleOrNull() ?: return null
    if (seconds <= 0.0) return null
    return (seconds * 1_000.0).roundToLong().coerceAtLeast(AuthoredPracticeStep.MinimumDurationMillis)
}

fun formatSeconds(durationMillis: Long): String {
    val seconds = durationMillis / 1_000.0
    return if (seconds % 1.0 == 0.0) {
        seconds.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", seconds)
    }
}

fun formatDurationMinutesSeconds(durationMillis: Long): String {
    val totalSeconds = ceil(durationMillis / 1_000.0).toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return when {
        minutes > 0L && seconds > 0L -> "${minutes}m ${seconds}s"
        minutes > 0L -> "${minutes}m"
        else -> "${seconds}s"
    }
}
