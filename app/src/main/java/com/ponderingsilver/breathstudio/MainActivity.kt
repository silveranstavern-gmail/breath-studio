package com.ponderingsilver.breathstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ponderingsilver.breathstudio.ui.theme.BreathStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BreathStudioTheme {
                BreathStudioApp()
            }
        }
    }
}
