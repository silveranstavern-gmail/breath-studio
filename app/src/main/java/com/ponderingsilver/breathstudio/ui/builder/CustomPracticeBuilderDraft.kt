package com.ponderingsilver.breathstudio.ui.builder

import com.ponderingsilver.breathstudio.domain.model.AuthoredBlockTarget
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeBlock
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeCycle
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeStep
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import com.ponderingsilver.breathstudio.domain.model.StepSound
import com.ponderingsilver.breathstudio.domain.model.defaultColorHexForLabel
import com.ponderingsilver.breathstudio.domain.model.normalizeColorHexOrDefault
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToLong

enum class BuilderTargetMode {
    DurationMinutes,
    Repetitions,
}

data class StepPreset(
    val label: String,
    val colorHex: String,
    val sound: StepSound,
)

val DefaultStepPresets: List<StepPreset> = listOf(
    StepPreset(label = "Inhale", colorHex = "#7ED9C8", sound = StepSound.Inhale),
    StepPreset(label = "Hold In", colorHex = "#E7C98C", sound = StepSound.Hold),
    StepPreset(label = "Exhale", colorHex = "#9BC1FF", sound = StepSound.Exhale),
    StepPreset(label = "Hold Out", colorHex = "#C7D7D4", sound = StepSound.Hold),
)

data class EditablePracticeStep(
    val id: String,
    val durationInput: String,
    val label: String = "",
    val colorHex: String = defaultColorHexForLabel(label),
    val sound: StepSound = StepSound.Default,
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
    val subtitle: String = "",
    val description: String = "",
    val category: String = "",
    val visualMode: BreathingVisualMode = BreathingVisualMode.Glow,
    val blocks: List<EditablePracticeBlock> = listOf(defaultEditableBlock()),
    val selectedBlockId: String = blocks.first().id,
)

val DefaultPracticeCategories: List<String> = listOf(
    "Calming",
    "Energizing",
    "Reset",
)

val CustomPracticeBuilderDraft.hasExistingPractice: Boolean
    get() = !practiceId.isNullOrBlank()

val CustomPracticeBuilderDraft.selectedBlock: EditablePracticeBlock
    get() = blocks.firstOrNull { it.id == selectedBlockId } ?: blocks.first()

fun defaultEditableSteps(): List<EditablePracticeStep> = DefaultStepPresets.mapIndexed { index, preset ->
    EditablePracticeStep(
        id = "step-$index-${preset.label.lowercase().replace(' ', '-')}",
        durationInput = "4",
        label = preset.label,
        colorHex = preset.colorHex,
        sound = preset.sound,
    )
}

fun defaultEditableBlock(): EditablePracticeBlock = EditablePracticeBlock(
    id = "block-main",
    title = "Main cycle",
    targetMode = BuilderTargetMode.DurationMinutes,
    targetValueInput = "5",
    steps = defaultEditableSteps(),
)

fun nextSuggestedPreset(previousLabel: String?): StepPreset {
    val normalized = previousLabel?.trim()?.lowercase().orEmpty()
    val previousIndex = DefaultStepPresets.indexOfFirst { it.label.lowercase() == normalized }
    val nextIndex = if (previousIndex == -1) 0 else (previousIndex + 1) % DefaultStepPresets.size
    return DefaultStepPresets[nextIndex]
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
                    durationInput = formatSeconds(step.safeDurationMillis),
                    label = step.label,
                    colorHex = step.safeColorHex,
                    sound = step.sound,
                )
            },
        )
    }
    return CustomPracticeBuilderDraft(
        practiceId = safeId,
        title = safeTitle,
        subtitle = subtitle,
        description = description,
        category = category,
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
        subtitle = subtitle.trim().ifBlank { draftSubtitle(authoredBlocks) },
        description = description.trim().ifBlank { draftDescription(authoredBlocks) },
        category = category.trim().ifBlank { DefaultPracticeCategories.first() },
        blocks = authoredBlocks,
        preferredVisualMode = visualMode,
        defaultDurationMinutes = suggestedDefaultDurationMinutes(authoredBlocks),
    )
}

fun EditablePracticeBlock.summaryLabel(): String {
    val cadence = steps.mapNotNull { step ->
        parseDurationMillis(step.durationInput)?.let { durationMillis ->
            val displayLabel = step.label.ifBlank { "Step" }
            "$displayLabel ${formatSeconds(durationMillis)}s"
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
        val safeLabel = step.label.trim().ifBlank { "Step" }
        AuthoredPracticeStep(
            durationMillis = durationMillis,
            label = safeLabel,
            colorHex = normalizeColorHexOrDefault(step.colorHex, safeLabel),
            sound = step.sound,
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
            "${step.label} ${formatSeconds(step.safeDurationMillis)}s"
        }
    }
    return "${blocks.size} block routine"
}

private fun draftDescription(blocks: List<AuthoredPracticeBlock.RepeatingCycle>): String {
    return blocks.joinToString(separator = " Then ") { block ->
        val cadence = block.cycle.steps.joinToString(", ") { step ->
            "${step.label.lowercase()} ${formatSeconds(step.safeDurationMillis)} seconds"
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

fun CustomPracticeBuilderDraft.updateSelectedBlock(
    transform: (EditablePracticeBlock) -> EditablePracticeBlock,
): CustomPracticeBuilderDraft {
    val updatedBlocks = blocks.map { block ->
        if (block.id == selectedBlockId) transform(block) else block
    }
    return copy(blocks = updatedBlocks)
}

fun List<EditablePracticeBlock>.insertBlockAfter(
    blockId: String,
    newBlock: EditablePracticeBlock,
): List<EditablePracticeBlock> {
    val index = indexOfFirst { it.id == blockId }
    if (index == -1) return this + newBlock
    return toMutableList().apply {
        add(index + 1, newBlock)
    }
}

fun List<EditablePracticeBlock>.moveBlock(
    blockId: String,
    direction: Int,
): List<EditablePracticeBlock> {
    val currentIndex = indexOfFirst { it.id == blockId }
    if (currentIndex == -1) return this
    val targetIndex = (currentIndex + direction).coerceIn(0, lastIndex)
    if (targetIndex == currentIndex) return this
    return toMutableList().apply {
        val item = removeAt(currentIndex)
        add(targetIndex, item)
    }
}

fun List<EditablePracticeStep>.replaceStep(
    stepId: String,
    transform: (EditablePracticeStep) -> EditablePracticeStep,
): List<EditablePracticeStep> = map { step ->
    if (step.id == stepId) transform(step) else step
}

fun List<EditablePracticeStep>.duplicateStepAfter(stepId: String): List<EditablePracticeStep> {
    val index = indexOfFirst { it.id == stepId }
    if (index == -1) return this
    val step = this[index]
    val duplicate = step.copy(id = UUID.randomUUID().toString())
    return toMutableList().apply {
        add(index + 1, duplicate)
    }
}

fun List<EditablePracticeStep>.moveStep(
    stepId: String,
    direction: Int,
): List<EditablePracticeStep> {
    val currentIndex = indexOfFirst { it.id == stepId }
    if (currentIndex == -1) return this
    val targetIndex = (currentIndex + direction).coerceIn(0, lastIndex)
    if (targetIndex == currentIndex) return this
    return toMutableList().apply {
        val item = removeAt(currentIndex)
        add(targetIndex, item)
    }
}

private const val MinimumEditableDurationSeconds = 0.1
private const val MaximumEditableDurationSeconds = 3_600.0
private const val MaximumDurationInputLength = 12

fun sanitizeDurationInput(input: String): String {
    if (input.isBlank()) return ""
    val filtered = buildString(input.length.coerceAtMost(MaximumDurationInputLength)) {
        var hasDecimalPoint = false
        input.forEach { character ->
            when {
                character.isDigit() -> append(character)
                character == '.' && !hasDecimalPoint -> {
                    hasDecimalPoint = true
                    append(character)
                }
            }
            if (length >= MaximumDurationInputLength) return@forEach
        }
    }
    return filtered
}

fun adjustDurationInputBySeconds(
    currentInput: String,
    deltaSeconds: Int,
): String {
    if (deltaSeconds == 0) return sanitizeDurationInput(currentInput)

    val currentSeconds = currentInput.trim().toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }
        ?: 1.0
    val adjustedSeconds = (currentSeconds + deltaSeconds)
        .coerceIn(MinimumEditableDurationSeconds, MaximumEditableDurationSeconds)
    val adjustedMillis = (adjustedSeconds * 1_000.0).roundToLong()
        .coerceAtLeast(AuthoredPracticeStep.MinimumDurationMillis)
    return sanitizeDurationInput(formatSeconds(adjustedMillis))
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
