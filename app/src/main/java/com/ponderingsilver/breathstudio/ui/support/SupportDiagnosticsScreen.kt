package com.ponderingsilver.breathstudio.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SupportDiagnosticsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val entries by SupportDiagnostics.entries.collectAsState()
    val environment = SupportDiagnostics.environmentSnapshot(context)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF091A21),
                        Color(0xFF122C35),
                        Color(0xFF18363D),
                    ),
                ),
            )
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("Back", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Support Debug",
                color = Color(0xFFEFDDB4),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x12FFFFFF),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Environment",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF8F4EA),
                )
                environment.forEach { (label, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xBDE0EAE7),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF8F4EA),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { SupportDiagnostics.clear() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFECD7AB),
                    contentColor = Color(0xFF153139),
                ),
            ) {
                Text("Clear log")
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF1E0BA),
                ),
            ) {
                Text("Back to support")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x10FFFFFF),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF8F4EA),
                )
                if (entries.isEmpty()) {
                    Text(
                        text = "No support diagnostics captured yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xDCE7EFEC),
                    )
                } else {
                    entries.asReversed().forEach { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0x12FFFFFF),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 0.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "${entry.timestampLabel}  ${entry.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFF1E0BA),
                                )
                                Text(
                                    text = entry.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF8F4EA),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
