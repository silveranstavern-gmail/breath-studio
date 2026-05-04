package com.ponderingsilver.breathstudio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                        canEdit = false,
                        onClick = { onAddPreset(preset) },
                    )
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
