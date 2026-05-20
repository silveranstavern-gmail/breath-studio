package com.ponderingsilver.breathstudio.ui.builder

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import com.ponderingsilver.breathstudio.ui.theme.contrastColor
import com.ponderingsilver.breathstudio.ui.theme.ensureVisible
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.ponderingsilver.breathstudio.domain.model.defaultColorHexForLabel
import com.ponderingsilver.breathstudio.domain.model.normalizeColorHexOrNull
import com.ponderingsilver.breathstudio.domain.model.StepSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    initialColor: Color,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val controller = rememberColorPickerController()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color(0xFFF8F3E8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pick a Step Color",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                controller = controller,
                initialColor = initialColor,
                onColorChanged = { colorEnvelope ->
                    if (colorEnvelope.fromUser) {
                        onColorSelected(colorEnvelope.color.toHex())
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFEBC58D))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7ED9C8),
                        contentColor = Color(0xFF133139)
                    )
                ) {
                    Text("Select Color")
                }
            }
        }
    }
}

private fun Color.toHex(): String = "#%02X%02X%02X".format(
    (this.red * 255).toInt(),
    (this.green * 255).toInt(),
    (this.blue * 255).toInt()
)

@Composable
fun BuilderHeader(
    isEditing: Boolean,
    blockCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = Color(0x0AFFFFFF),
            contentColor = Color(0xFFF6EEDB).copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x1AFFFFFF))
        ) {
            Text(
                text = "Custom Builder",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
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
            color = Color(0x0AFFFFFF),
            contentColor = Color(0xFFF4DEB5).copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x12FFFFFF))
        ) {
            Text(
                text = "$blockCount block${if (blockCount == 1) "" else "s"} in this routine",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun BuilderSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
fun BlockSummaryCard(
    block: EditablePracticeBlock,
    index: Int,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
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
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val cycleDuration =
                    block.cycleDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "Invalid"
                val totalStageDuration =
                    block.estimatedBlockDurationMillisOrNull()?.let(::formatDurationMinutesSeconds) ?: "Invalid"
                Text(
                    text = "Cycle Duration: $cycleDuration",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFEFDDB4),
                )
                Text(
                    text = "Total Stage Duration: $totalStageDuration",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFEFDDB4),
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
                if (selected) {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
fun StepEditorCard(
    index: Int,
    step: EditablePracticeStep,
    canRemove: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPresetSelected: (StepPreset) -> Unit,
    onLabelChanged: (String) -> Unit,
    onColorHexChanged: (String) -> Unit,
    onSoundChanged: (StepSound) -> Unit,
    onDurationChanged: (String) -> Unit,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val normalizedColorHex = normalizeColorHexOrNull(step.colorHex) ?: defaultColorHexForLabel(step.label)
    val previewColor = colorFromHex(normalizedColorHex)
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerBottomSheet(
            initialColor = previewColor,
            onColorSelected = onColorHexChanged,
            onDismiss = { showColorPicker = false }
        )
    }

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
                DefaultStepPresets.forEach { preset ->
                    BuilderChoiceChip(
                        title = preset.label,
                        caption = if (step.label.trim().equals(preset.label, ignoreCase = true)) "Selected" else "Tap to use",
                        selected = step.label.trim().equals(preset.label, ignoreCase = true),
                        onClick = { onPresetSelected(preset) },
                    )
                }
            }
            OutlinedTextField(
                value = step.label,
                onValueChange = onLabelChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Step") },
                label = { Text("Display label") },
                supportingText = {
                    Text("What the user sees during the session.")
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Step sound",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xDDEBE6DA),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StepSound.entries.forEach { sound ->
                        BuilderChoiceChip(
                            title = sound.label,
                            caption = when (sound) {
                                StepSound.Default -> "Soft bell"
                                StepSound.Inhale -> "Breath in"
                                StepSound.Exhale -> "Breath out"
                                StepSound.Hold -> "Hold cue"
                                StepSound.Other1 -> "Soft cue A"
                                StepSound.Other2 -> "Soft cue B"
                            },
                            selected = step.sound == sound,
                            onClick = { onSoundChanged(sound) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DefaultStepPresets.forEach { preset ->
                    val presetHex = preset.colorHex
                    val isColorMatchingPreset = normalizedColorHex.equals(presetHex, ignoreCase = true)

                    BuilderChoiceChip(
                        title = preset.label,
                        caption = if (isColorMatchingPreset) "Preset active" else presetHex,
                        selected = isColorMatchingPreset,
                        onClick = { onColorHexChanged(presetHex) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = step.colorHex,
                    onValueChange = { onColorHexChanged(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Step Color HEX") },
                    placeholder = { Text("#7ED9C8") },
                    supportingText = { Text("Use #RRGGBB (e.g. #9BC1FF).") },
                    trailingIcon = {
                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Pick color",
                                tint = previewColor.ensureVisible(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                )
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { showColorPicker = true }
                        .clip(RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = previewColor.copy(alpha = 0.24f),
                    contentColor = previewColor.contrastColor(),
                ) {
                    Text(
                        text = normalizedColorHex,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            DurationStepper(
                value = step.durationInput,
                onValueChanged = onDurationChanged,
                onStepDown = {
                    onDurationChanged(adjustDurationInputBySeconds(step.durationInput, -1))
                },
                onStepUp = {
                    onDurationChanged(adjustDurationInputBySeconds(step.durationInput, 1))
                },
            )
        }
    }
}

@Composable
private fun DurationStepper(
    value: String,
    onValueChanged: (String) -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Seconds",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xDDEBE6DA),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DurationStepButton(
                text = "-",
                onClick = onStepDown,
            )
            OutlinedTextField(
                value = value,
                onValueChange = { nextValue ->
                    onValueChanged(sanitizeDurationInput(nextValue))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(DurationStepperHeight),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                placeholder = {
                    Text(
                        text = "4",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
            )
            DurationStepButton(
                text = "+",
                onClick = onStepUp,
            )
        }
        Text(
            text = "Use decimals (e.g. 0.5s).",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xBCE0EAE7),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DurationStepButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(DurationStepperHeight)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x66E9DDC2), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFFF8F0DE),
            textAlign = TextAlign.Center,
        )
    }
}

private val DurationStepperHeight = 56.dp

private fun colorFromHex(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF9CCBE9))
}

@Composable
fun TimingSummaryCard(
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
fun BuilderChoiceChip(
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
