package com.ponderingsilver.breathstudio.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ponderingsilver.breathstudio.domain.model.AuthoredPracticeDefinition
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
    val stepsSectionRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
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
                title = "Library details",
                subtitle = "Use an intention label that fits how the session should feel in the library.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DefaultPracticeCategories.forEach { presetCategory ->
                            BuilderChoiceChip(
                                title = presetCategory,
                                caption = if (draft.category.trim().equals(presetCategory, ignoreCase = true)) "Selected" else "Library label",
                                selected = draft.category.trim().equals(presetCategory, ignoreCase = true),
                                onClick = {
                                    draft = draft.copy(category = presetCategory)
                                },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draft.category,
                        onValueChange = { draft = draft.copy(category = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Intention") },
                        placeholder = { Text("Calming, Energizing, Reset...") },
                    )
                    OutlinedTextField(
                        value = draft.subtitle,
                        onValueChange = { draft = draft.copy(subtitle = it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Subtitle") },
                        placeholder = { Text("Steady focus") },
                    )
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { draft = draft.copy(description = it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Description") },
                        placeholder = { Text("A short library description for this session.") },
                    )
                }
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
                            onEdit = {
                                draft = draft.copy(selectedBlockId = block.id)
                                coroutineScope.launch {
                                    stepsSectionRequester.bringIntoView()
                                }
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
                modifier = Modifier.bringIntoViewRequester(stepsSectionRequester),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    selectedBlock.steps.forEachIndexed { index, step ->
                        StepEditorCard(
                            index = index,
                            step = step,
                            canRemove = selectedBlock.steps.size > 1,
                            canMoveUp = index > 0,
                            canMoveDown = index < selectedBlock.steps.lastIndex,
                            onPresetSelected = { preset ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = {
                                                it.copy(
                                                    label = preset.label,
                                                    colorHex = preset.colorHex,
                                                    sound = preset.sound,
                                                )
                                            },
                                        ),
                                    )
                                }
                            },
                            onLabelChanged = { label ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = { it.copy(label = label) },
                                        ),
                                    )
                                }
                            },
                            onSoundChanged = { sound ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = { it.copy(sound = sound) },
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
                            onColorHexChanged = { colorHex ->
                                draft = draft.updateSelectedBlock { block ->
                                    block.copy(
                                        steps = block.steps.replaceStep(
                                            stepId = step.id,
                                            transform = { editableStep ->
                                                editableStep.copy(
                                                    colorHex = colorHex,
                                                )
                                            },
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
                                val nextPreset = nextSuggestedPreset(block.steps.lastOrNull()?.label)
                                block.copy(
                                    steps = block.steps + EditablePracticeStep(
                                        id = UUID.randomUUID().toString(),
                                        label = nextPreset.label,
                                        durationInput = "4",
                                        colorHex = nextPreset.colorHex,
                                        sound = nextPreset.sound,
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
