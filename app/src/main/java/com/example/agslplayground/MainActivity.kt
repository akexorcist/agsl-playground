package com.example.agslplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = dynamicDarkColorScheme(LocalContext.current)) {
                var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
                val selected = selectedId?.let(ShaderSamples::byId)

                if (selected == null) {
                    SampleListScreen(onOpen = { selectedId = it.id })
                } else {
                    BackHandler { selectedId = null }
                    PlaygroundScreen(sample = selected, onBack = { selectedId = null })
                }
            }
        }
    }
}
