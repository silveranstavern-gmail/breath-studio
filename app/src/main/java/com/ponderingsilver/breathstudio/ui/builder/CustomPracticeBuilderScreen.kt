package com.ponderingsilver.breathstudio.ui.builder

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import com.ponderingsilver.breathstudio.domain.model.BreathAction
import com.ponderingsilver.breathstudio.domain.model.BreathingVisualMode
import java.util.UUID

@Composable
fun CustomPracticeBuilderScreen(
    initialDefinition: AuthoredPracticeDefinition? = null,
    onCancel: () -> Unit,
    onSave: suspend (AuthoredPracticeDefinition) -> Boolean,
) {
    var draft by remember(initialDefinition) {
        mutableStateOf(initialDefinition?.toBuilderDraftOrNull() ?: CustomPracticeBuilderDraft())
    }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val selectedBlock = draft.selectedBlock
    val totalDurationMillis = draft.estimatedTotalDurationMillisOrNull()
    val isEditing = draft.hasExistingPractice
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08171D),
                Color(0xFF0E2830),
                Color(0xFF14353B),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BuilderHeader(isEditing = isEditing, blockCount = draft.blocks.size)
            BuilderSection(
                title = "Practice title",
                subtitle = "Keep this recognizable in the library. The blocks below describe the routine itself.",
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Custom Practice") },
                )
            }
            BuilderSection(
                title = "Session blocks",
                subtitle = "Build the routine by stacking simple repeating cycles. Duplicate and tweak instead of rebuilding.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    draft.blocks.forEachIndexed { index, block ->
                        BlockSummaryCard(
                            block = block,
                            index = index,
                            selected = block.id == draft.selectedBlockId,
                            canMoveUp = index > 0,
                            canMoveDown = index < draft.blocks.lastIndex,
                            canRemove = draft.blocks.size > 1,
                            onSelect = {
                                draft = draft.copy(selectedBlockId = block.id)
                            },
                            onDuplicate = {
                                val duplicate = newEditableBlockFromTemplate(block)
                                draft = draft.copy(
                                    blocks = draft.blocks.insertBlockAfter(block.id, duplicate),
                                    selectedBlockId = duplicate.id,
                                )
                            },
                            onMoveUp = {
                                draft = draft.copy(blocks = draft.blocks.moveBlock(block.id, -1))
                            },
                            onMoveDown = {
                                draft = draft.copy(blocks = draft.blocks.moveBlock(block.id, 1))
                            },
                            onRemove = {
                                val remaining = draft.blocks.filterNot { it.id == block.id }
                                draft = draft.copy(
                                    blocks = remaining,
                                    selectedBlockId = remaining.first().id,
                                )
                            },
                        )
                    }
                    TextButton(
                        onClick = {
                            val template = if (draft.blocks.isNotEmpty()) draft.selectedBlock else null
                            val newBlock = newEditableBlockFromTemplate(template)
                            draft = draft.copy(
                                blocks = draft.blocks + newBlock,
                                selectedBlockId = newBlock.id,
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    ) {
                        Text("Add block")
                    }
                }
            }
            BuilderSection(
                title = "Whole session",
                subtitle = "Keep the session understandable at a glance before you dive into the selected block.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimingSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Blocks",
                        value = draft.blocks.size.toString(),
                        caption = draft.summaryLabel(),
                    )
                    TimingSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Session estimate",
                        value = totalDurationMillis?.let(::formatDurationMinutesSeconds) ?: "Invalid",
                        caption = "Across all blocks",
                    )
                }
            }
            BuilderSection(
                title = "Selected block",
                subtitle = "Focus on one block at a time. This keeps complex routines editable without turning the builder into a spreadsheet.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selectedBlock.title,
                        onValueChange = { title ->
                            draft = draft.updateSelectedBlock { it.copy(title = title) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Block title") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimingSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Cycle length",
                            value = selectedBlock.cycleDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "Invalid",
                            caption = "${selectedBlock.steps.size} step${if (selectedBlock.steps.size == 1) "" else "s"}",
                        )
                        TimingSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Block estimate",
                            value = selectedBlock.estimatedBlockDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "Invalid",
                            caption = selectedBlock.summaryLabel(),
                        )
                    }
                }
            }
            BuilderSection(
                title = "Run target",
                subtitle = "Choose whether the selected block runs for total minutes or a fixed round count.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BuilderChoiceChip(
                            title = "Minutes",
                            caption = "Timed block",
                            selected = selectedBlock.targetMode == BuilderTargetMode.DurationMinutes,
                            onClick = {
                                draft = draft.updateSelectedBlock {
                                    it.copy(
                                        targetMode = BuilderTargetMode.DurationMinutes,
                                        targetValueInput = it.targetValueInput.ifBlank { "5" },
                                    )
                                }
                            },
                        )
                        BuilderChoiceChip(
                            title = "Rounds",
                            caption = "Cycle count",
                            selected = selectedBlock.targetMode == BuilderTargetMode.Repetitions,
                            onClick = {
                                draft = draft.updateSelectedBlock {
                                    it.copy(
                                        targetMode = BuilderTargetMode.Repetitions,
                                        targetValueInput = it.targetValueInput.ifBlank { "10" },
                                    )
                                }
                            },
                        )
                    }
                    OutlinedTextField(
                        value = selectedBlock.targetValueInput,
                        onValueChange = { value ->
                            draft = draft.updateSelectedBlock {
                                it.copy(targetValueInput = value.filter { character -> character.isDigit() })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default,
                        label = {
                            Text(if (selectedBlock.targetMode == BuilderTargetMode.DurationMinutes) "Minutes" else "Rounds")
                        },
                    )
                }
            }
            BuilderSection(
                title = "Cycle steps",
                subtitle = "Tweak the selected block, then duplicate it if the next section is just a variation.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    selectedBlock.steps.forEachIndexed { index, step ->
                        StepEditorCard(
                            index = index,
                            step = step,
                            canRemove = selectedBlock.steps.size > 1,
                            canMoveUp = index > 0,
                            canMoveDown = index < selectedBlock.steps.lastIndex,
                            onActionSelected = { action ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = { it.copy(action = action) },
                                        ),
                                    )
                                }
                            },
                            onDurationChanged = { duration ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = { it.copy(durationInput = duration) },
                                        ),
                                    )
                                }
                            },
                            onRemove = {
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(steps = block.steps.filterNot { it.id == step.id })
                                }
                            },
                            onDuplicate = {
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(steps = block.steps.duplicateStepAfter(step.id))
                                }
                            },
                            onMoveUp = {
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(steps = block.steps.moveStep(step.id, -1))
                                }
                            },
                            onMoveDown = {
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(steps = block.steps.moveStep(step.id, 1))
                                }
                            },
                        )
                    }
                    TextButton(
                        onClick = {
                            draft = draft.updateSelectedBlock { block ->
                                block.copy(
                                    steps = block.steps + EditablePracticeStep(
                                        id = UUID.randomUUID().toString(),
                                        action = nextSuggestedAction(block.steps.lastOrNull()?.action),
                                        durationInput = "4",
                                    ),
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                    ) {
                        Text("Add step")
                    }
                }
            }
            BuilderSection(
                title = "Visual guide",
                subtitle = "Save the routine with its default visual preference.",
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BreathingVisualMode.entries.forEach { mode ->
                        BuilderChoiceChip(
                            title = mode.label,
                            caption = if (mode == BreathingVisualMode.Circle) "Expanding guide" else "Square tracer",
                            selected = draft.visualMode == mode,
                            onClick = { draft = draft.copy(visualMode = mode) },
                        )
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x10FFFFFF),
                    contentColor = Color(0xFFF7F2E7),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = draft.summaryLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xDDE6EFEC),
                    )
                    saveError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF2B88D),
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val definition = draft.toAuthoredPracticeDefinitionOrNull {
                        "custom-${UUID.randomUUID()}"
                    }
                    if (definition == null) {
                        saveError = "Enter a valid target and a positive duration for every block step."
                        return@Button
                    }
                    isSaving = true
                    saveError = null
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(vertical = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFECD7AB),
                    contentColor = Color(0xFF153139),
                ),
            ) {
                Text(
                    text = if (isSaving) {
                        "Saving..."
                    } else if (isEditing) {
                        "Update custom practice"
                    } else {
                        "Save custom practice"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
            ) {
                Text(
                    text = "Back",
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
        }
    }

    if (isSaving) {
        val definition = remember(draft) {
            draft.toAuthoredPracticeDefinitionOrNull {
                "custom-${UUID.randomUUID()}"
            }
        }
        LaunchedEffect(definition) {
            val saved = if (definition != null) onSave(definition) else false
            isSaving = false
            if (saved) {
                onCancel()
            } else {
                saveError = "The custom practice could not be saved."
            }
        }
    }
}

@Composable
private fun BuilderHeader(
    isEditing: Boolean,
    blockCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = Color(0x18FFFFFF),
            contentColor = Color(0xFFF6EEDB),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = "Custom Builder",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = if (isEditing) "Edit your custom routine" else "Create a custom routine",
            style = MaterialTheme.typography.displaySmall,
            color = Color(0xFFF8F3E8),
            lineHeight = 44.sp,
        )
        Text(
            text = if (isEditing) {
                "Refine the selected saved practice without rebuilding it from scratch."
            } else {
                "Build one focused block first, then duplicate and tweak it into a larger routine."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xD6EBF1ED),
            lineHeight = 24.sp,
        )
        Surface(
            color = Color(0x14FFFFFF),
            contentColor = Color(0xFFF4DEB5),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                text = "$blockCount block${if (blockCount == 1) "" else "s"} in this routine",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun BuilderSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFFF7F1E5),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xCCE0EAE7),
        )
        content()
    }
}

@Composable
private fun BlockSummaryCard(
    block: EditablePracticeBlock,
    index: Int,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFECD7AB) else Color(0x24FFFFFF),
        label = "block-card-border",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0x16FFFFFF) else Color(0x10FFFFFF),
            contentColor = Color(0xFFF7F2E8),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = block.title.ifBlank { "Block ${index + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = block.summaryLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xDDE6EFEC),
                    )
                }
                SelectionPill(text = if (selected) "Editing" else "Block ${index + 1}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionPill(
                    text = block.cycleDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "Invalid cycle",
                )
                SelectionPill(
                    text = block.estimatedBlockDurationMillisOrNull()?.let(::formatDurationMinutesSeconds)
                        ?: "Invalid block",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canMoveUp) {
                    TextButton(onClick = onMoveUp, contentPadding = PaddingValues(0.dp)) {
                        Text("Up")
                    }
                }
                if (canMoveDown) {
                    TextButton(onClick = onMoveDown, contentPadding = PaddingValues(0.dp)) {
                        Text("Down")
                    }
                }
                TextButton(onClick = onDuplicate, contentPadding = PaddingValues(0.dp)) {
                    Text("Copy")
                }
                if (canRemove) {
                    TextButton(onClick = onRemove, contentPadding = PaddingValues(0.dp)) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepEditorCard(
    index: Int,
    step: EditablePracticeStep,
    canRemove: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onActionSelected: (BreathAction) -> Unit,
    onDurationChanged: (String) -> Unit,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x14FFFFFF),
            contentColor = Color(0xFFF7F2E8),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Step ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (canMoveUp) {
                        TextButton(onClick = onMoveUp, contentPadding = PaddingValues(0.dp)) {
                            Text("Up")
                        }
                    }
                    if (canMoveDown) {
                        TextButton(onClick = onMoveDown, contentPadding = PaddingValues(0.dp)) {
                            Text("Down")
                        }
                    }
                    TextButton(onClick = onDuplicate, contentPadding = PaddingValues(0.dp)) {
                        Text("Copy")
                    }
                    if (canRemove) {
                        TextButton(onClick = onRemove, contentPadding = PaddingValues(0.dp)) {
                            Text("Remove")
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BreathAction.entries.forEach { action ->
                    BuilderChoiceChip(
                        title = action.label,
                        caption = if (action == step.action) "Selected" else "Tap to use",
                        selected = action == step.action,
                        onClick = { onActionSelected(action) },
                    )
                }
            }
            OutlinedTextField(
                value = step.durationInput,
                onValueChange = { value ->
                    onDurationChanged(value.filter { it.isDigit() || it == '.' })
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                label = { Text("Seconds") },
                supportingText = {
                    Text(
                        text = "Sub-second values like 0.5 are supported.",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun TimingSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    caption: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x12FFFFFF),
            contentColor = Color(0xFFF6F2E8),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xCCE0EAE7),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xCCDDE7E5),
            )
        }
    }
}

@Composable
private fun BuilderChoiceChip(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFECD7AB) else Color(0x16FFFFFF),
        label = "builder-chip-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF133139) else Color(0xFFF1F5F2),
        label = "builder-chip-content",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Color(0x20FFFFFF),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun SelectionPill(text: String) {
    Surface(
        color = Color(0x18FFFFFF),
        contentColor = Color(0xFFEFDDB4),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun CustomPracticeBuilderDraft.updateSelectedBlock(
    transform: (EditablePracticeBlock) -> EditablePracticeBlock,
): CustomPracticeBuilderDraft {
    val updatedBlocks = blocks.map { block ->
        if (block.id == selectedBlockId) transform(block) else block
    }
    return copy(blocks = updatedBlocks)
}

private fun List<EditablePracticeBlock>.insertBlockAfter(
    blockId: String,
    newBlock: EditablePracticeBlock,
): List<EditablePracticeBlock> {
    val index = indexOfFirst { it.id == blockId }
    if (index == -1) return this + newBlock
    return toMutableList().apply {
        add(index + 1, newBlock)
    }
}

private fun List<EditablePracticeBlock>.moveBlock(
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

private fun List<EditablePracticeStep>.replaceStep(
    stepId: String,
    transform: (EditablePracticeStep) -> EditablePracticeStep,
): List<EditablePracticeStep> = map { step ->
    if (step.id == stepId) transform(step) else step
}

private fun List<EditablePracticeStep>.duplicateStepAfter(stepId: String): List<EditablePracticeStep> {
    val index = indexOfFirst { it.id == stepId }
    if (index == -1) return this
    val step = this[index]
    val duplicate = step.copy(id = UUID.randomUUID().toString())
    return toMutableList().apply {
        add(index + 1, duplicate)
    }
}

private fun List<EditablePracticeStep>.moveStep(
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
