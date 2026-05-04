package com.ponderingsilver.breathstudio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ponderingsilver.breathstudio.domain.model.BreathPractice

@Composable
fun PresetPickerScreen(
    presets: List<BreathPractice>,
    onAddPreset: (BreathPractice) -> Unit,
    onBack: () -> Unit,
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
            .background(backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle(
                title = "Add preset",
                subtitle = "Import a preset into your saved library, then edit or delete it like any other session.",
            )
            if (presets.isEmpty()) {
                EmptyLibraryCard(
                    title = "All presets are already in your library.",
                    subtitle = "Go back to keep editing or start from scratch with a custom session.",
                )
            } else {
                presets.forEach { preset ->
                    PracticeCard(
                        practice = preset,
                        selected = false,
                        onClick = { onAddPreset(preset) },
                        trailingLabel = "Preset",
                    )
                    Button(
                        onClick = { onAddPreset(preset) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x18FFFFFF),
                            contentColor = Color(0xFFF1E0BA),
                        ),
                    ) {
                        Text(
                            text = "Add ${preset.safeTitle}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back")
            }
        }
    }
}
