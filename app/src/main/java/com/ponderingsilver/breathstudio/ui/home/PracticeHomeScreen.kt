package com.ponderingsilver.breathstudio.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ponderingsilver.breathstudio.domain.model.BreathPractice

@Composable
fun PracticeHomeScreen(
    practices: List<BreathPractice>,
    selectedPractice: BreathPractice,
    onPracticeSelected: (BreathPractice) -> Unit,
    onStartSession: () -> Unit,
    onCreateCustomPractice: () -> Unit,
    onEditSelectedPractice: () -> Unit,
    canEditSelectedPractice: Boolean,
) {
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
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(
                title = "Start a practice",
                subtitle = "Select a saved or built-in practice. The practice controls its own duration and guide.",
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                practices.forEach { practice ->
                    PracticeCard(
                        practice = practice,
                        selected = practice.safeId == selectedPractice.safeId,
                        onClick = { onPracticeSelected(practice) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreateCustomPractice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x18FFFFFF),
                    contentColor = Color(0xFFF1E0BA),
                ),
            ) {
                Text(
                    text = "Create custom practice",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (canEditSelectedPractice) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onEditSelectedPractice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x10FFFFFF),
                        contentColor = Color(0xFFF7F1E5),
                    ),
                ) {
                    Text(
                        text = "Edit selected custom practice",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            Spacer(modifier = Modifier.height(26.dp))
            Button(
                onClick = onStartSession,
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
            Text(
                text = "${selectedPractice.safeDefaultDurationMinutes} min • ${selectedPractice.safeCategory} • ${selectedPractice.preferredVisualMode.label} • ${selectedPractice.cadenceLabel}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xCCE7EFEA),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HomeHero(
    selectedPractice: BreathPractice,
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
            Surface(
                color = Color(0x18FFFFFF),
                contentColor = Color(0xFFF6EEDB),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "Breath Studio",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = "A breathing app, not a wellness catalog.",
                style = MaterialTheme.typography.displaySmall,
                color = Color(0xFFF9F4E8),
                lineHeight = 46.sp,
            )
            Text(
                text = "Start quickly, follow strong motion, and build toward richer practices without making the app feel technical.",
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
                    text = "Featured now: ${selectedPractice.safeTitle}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
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
private fun PracticeCard(
    practice: BreathPractice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (practice.safeCategory) {
        "Focus" -> Color(0xFF88D4D0)
        "Sleep" -> Color(0xFFD9B784)
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
                SelectionPill(text = if (selected) "Selected" else practice.safeCategory)
            }
            Text(
                text = practice.safeDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xDCE7EFEC),
                lineHeight = 23.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionPill(text = "${practice.stages.size} stage${if (practice.stages.size == 1) "" else "s"}")
                SelectionPill(text = practice.preferredVisualMode.label)
            }
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
