package com.ponderingsilver.breathstudio.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ponderingsilver.breathstudio.data.practice.PracticeLibraryEntry
import com.ponderingsilver.breathstudio.domain.model.BreathPractice
import com.ponderingsilver.breathstudio.ui.components.SelectionPill
import com.ponderingsilver.breathstudio.ui.builder.formatDurationMinutesSeconds

@Composable
fun PracticeHomeScreen(
    entries: List<PracticeLibraryEntry>,
    selectedPractice: BreathPractice?,
    onPracticeSelected: (BreathPractice) -> Unit,
    onStartSession: (BreathPractice) -> Unit,
    onAddPreset: () -> Unit,
    onCreateCustomPractice: () -> Unit,
    onCopyPractice: (String) -> Unit,
    onEditPractice: (String) -> Unit,
    onDeleteSelectedPractice: () -> Unit,
    onOpenSupport: () -> Unit,
    canManageSelectedPractice: Boolean,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF091A21),
                Color(0xFF102E37),
                Color(0xFF17393F),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x30FFE4B8), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.10f),
                        radius = size.minDimension * 0.44f,
                    ),
                    radius = size.minDimension * 0.44f,
                    center = Offset(size.width * 0.12f, size.height * 0.10f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2255DCCC), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.28f),
                        radius = size.minDimension * 0.5f,
                    ),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width * 0.86f, size.height * 0.28f),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(scrollState),
        ) {
            HomeHero(selectedPractice = selectedPractice)
            Spacer(modifier = Modifier.height(16.dp))
            SupportIntroCard(onOpenSupport = onOpenSupport)
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(
                title = "Your library",
                subtitle = "Saved sessions all run through the same flow. Add a preset, build your own, then edit or delete from here.",
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (entries.isEmpty()) {
                EmptyLibraryCard(
                    title = "Your studio is quiet. Start by adding a preset.",
                    subtitle = "Add a preset or create a custom session to start building it.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entries.forEach { entry ->
                        val practice = entry.practice
                        val isSelected = practice.safeId == selectedPractice?.safeId
                        PracticeCard(
                            practice = practice,
                            selected = isSelected,
                            canEdit = entry.canEdit,
                            onClick = {
                                onPracticeSelected(practice)
                                if (!isSelected) showDeleteConfirmation = false
                            },
                            onBegin = if (isSelected) {
                                { onStartSession(practice) }
                            } else {
                                null
                            },
                            onCopy = if (isSelected) {
                                { onCopyPractice(practice.safeId) }
                            } else {
                                null
                            },
                            onEdit = if (isSelected && entry.canEdit) {
                                { onEditPractice(practice.safeId) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddPreset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x18FFFFFF),
                    contentColor = Color(0xFFF1E0BA),
                ),
            ) {
                Text(
                    text = "Add preset",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onCreateCustomPractice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x10FFFFFF),
                    contentColor = Color(0xFFF1E0BA),
                ),
            ) {
                Text(
                    text = "Create custom session",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (canManageSelectedPractice) {
                Spacer(modifier = Modifier.height(10.dp))
                if (showDeleteConfirmation) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF7F1E5),
                            ),
                        ) {
                            Text(
                                text = "Cancel delete",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Button(
                            onClick = {
                                onDeleteSelectedPractice()
                                showDeleteConfirmation = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x18A84444),
                                contentColor = Color(0xFFFFE1D7),
                            ),
                        ) {
                            Text(
                                text = "Confirm delete",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x14A84444),
                            contentColor = Color(0xFFFFE1D7),
                        ),
                    ) {
                        Text(
                            text = "Delete selected session",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
            if (selectedPractice != null) {
                Button(
                    onClick = { onStartSession(selectedPractice) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(vertical = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFECD7AB),
                        contentColor = Color(0xFF153139),
                    ),
                ) {
                    Text(
                        text = "Begin ${selectedPractice.safeTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val displayDuration = formatDurationMinutesSeconds(selectedPractice.cycleDurationSeconds * 1000L)
                Text(
                    text = "$displayDuration • ${selectedPractice.safeCategory} • ${selectedPractice.cadenceLabel}",                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xCCE7EFEA),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Add or create a session to start practicing.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xCCE7EFEA),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SupportIntroCard(
    onOpenSupport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x12FFFFFF),
        contentColor = Color(0xFFF7F1E5),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Free, ad-free, and supported by care.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF8F4EA),
            )
            Text(
                text = "While this app is free and doesn't use ads, we don't have UBI yet. Please consider supporting me if you found value in this app. I know many are struggling right now, so please give only if you're in a place of abundance right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xDCE7EFEC),
                lineHeight = 21.sp,
            )
            Button(
                onClick = onOpenSupport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFECD7AB),
                    contentColor = Color(0xFF153139),
                ),
            ) {
                Text(
                    text = "Support options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HomeHero(
    selectedPractice: BreathPractice?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x26FFF2D7),
                        Color(0x1457E1D4),
                    ),
                ),
            )
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(34.dp))
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Breath Studio",
                style = MaterialTheme.typography.displaySmall,
                color = Color(0xFFF9F4E8),
                lineHeight = 46.sp,
            )
            Text(
                text = "Designed for practice, crafted with intention, given with love.",
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
                    text = selectedPractice?.let { "Next Practice: ${it.safeTitle}" } ?: "Build a library you can actually reuse",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = Color(0xFFF7F1E5),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = subtitle,
            color = Color(0xCCE0EAE7),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun PracticeCard(
    practice: BreathPractice,
    selected: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onBegin: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val accent = when (practice.safeCategory) {
        "Calming" -> Color(0xFFD9B784)
        "Energizing" -> Color(0xFF88D4D0)
        "Reset" -> Color(0xFF7FD0AA)
        else -> Color(0xFFB2D4F1)
    }
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else Color(0x24FFFFFF),
        label = "practice-border",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = if (selected) {
                        listOf(accent.copy(alpha = 0.24f), Color(0x14FFFFFF))
                    } else {
                        listOf(Color(0x14FFFFFF), Color(0x0CFFFFFF))
                    },
                ),
            )
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = practice.safeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF8F4EA),
                    )
                    Text(
                        text = practice.safeSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
            }
            Text(
                text = practice.safeDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xDCE7EFEC),
                lineHeight = 23.sp,
            )
            SelectionPill(
                text = practice.safeCategory,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (onCopy != null) {
                    ActionPillButton(text = "Copy", onClick = onCopy)
                }
                if (canEdit && onEdit != null) {
                    ActionPillButton(text = "Edit", onClick = onEdit)
                }
                if (onBegin != null) {
                    ActionPillButton(text = "Begin", onClick = onBegin)
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x22FFFFFF))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF4DEB5),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EmptyLibraryCard(
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x10FFFFFF),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF8F4EA),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xDCE7EFEC),
            )
        }
    }
}
