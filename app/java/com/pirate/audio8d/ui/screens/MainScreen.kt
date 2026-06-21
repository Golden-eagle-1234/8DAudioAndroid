package com.pirate.audio8d.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pirate.audio8d.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val orbitSpeed by viewModel.orbitSpeed.collectAsState()
    val depth by viewModel.depth.collectAsState()
    val reverbMix by viewModel.reverbMix.collectAsState()
    val delayMix by viewModel.delayMix.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val mode by viewModel.mode.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("8D Pirate Processor") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode: ${if (mode == "system") "System-wide" else "Player"}")
                IconButton(onClick = {
                    val newMode = if (mode == "system") "player" else "system"
                    viewModel.setMode(newMode)
                }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Toggle Mode")
                }
            }

            // Sliders
            LabeledSlider("Orbit Speed (Hz)", value = orbitSpeed, range = 0.05f..0.8f) {
                viewModel.setOrbitSpeed(it)
            }
            LabeledSlider("Depth", value = depth, range = 0f..1f) {
                viewModel.setDepth(it)
            }
            LabeledSlider("Reverb Mix", value = reverbMix, range = 0f..1f) {
                viewModel.setReverbMix(it)
            }
            LabeledSlider("Delay Mix", value = delayMix, range = 0f..1f) {
                viewModel.setDelayMix(it)
            }
            LabeledSlider("Bass Boost (dB)", value = bassBoost, range = 0f..12f) {
                viewModel.setBassBoost(it)
            }

            // Save preset button
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Preset")
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetName.isNotBlank()) {
                        viewModel.saveCurrentPreset(presetName)
                        showSaveDialog = false
                        presetName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = "$label: ${"%.2f".format(value)}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}