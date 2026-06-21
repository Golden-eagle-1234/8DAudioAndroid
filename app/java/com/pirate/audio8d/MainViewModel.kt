package com.pirate.audio8d

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val presetStore = PresetDataStore(application)
    private val _orbitSpeed = MutableStateFlow(0.15f)
    private val _depth = MutableStateFlow(0.8f)
    private val _reverbMix = MutableStateFlow(0.4f)
    private val _delayMix = MutableStateFlow(0.2f)
    private val _bassBoost = MutableStateFlow(4.0f)
    private val _mode = MutableStateFlow("system") // "system" or "player"

    val orbitSpeed: StateFlow<Float> = _orbitSpeed
    val depth: StateFlow<Float> = _depth
    val reverbMix: StateFlow<Float> = _reverbMix
    val delayMix: StateFlow<Float> = _delayMix
    val bassBoost: StateFlow<Float> = _bassBoost
    val mode: StateFlow<String> = _mode

    init {
        // Load last preset
        viewModelScope.launch {
            presetStore.currentSettings.collect { preset ->
                _orbitSpeed.value = preset.orbitSpeed
                _depth.value = preset.depth
                _reverbMix.value = preset.reverbMix
                _delayMix.value = preset.delayMix
                _bassBoost.value = preset.bassBoost
            }
        }
    }

    fun setOrbitSpeed(speed: Float) {
        _orbitSpeed.value = speed
        NativeBridge.setOrbitSpeed(speed)
    }

    fun setDepth(depth: Float) {
        _depth.value = depth
        NativeBridge.setDepth(depth)
    }

    fun setReverbMix(mix: Float) {
        _reverbMix.value = mix
        NativeBridge.setReverbMix(mix)
    }

    fun setDelayMix(mix: Float) {
        _delayMix.value = mix
        NativeBridge.setDelayMix(mix)
    }

    fun setBassBoost(db: Float) {
        _bassBoost.value = db
        NativeBridge.setBassBoost(db)
    }

    fun setMode(newMode: String) {
        _mode.value = newMode
        // Restart service with new mode
        restartService()
    }

    fun saveCurrentPreset(name: String) {
        viewModelScope.launch {
            presetStore.savePreset(name, AudioPreset(
                name, _orbitSpeed.value, _depth.value, _reverbMix.value,
                _delayMix.value, _bassBoost.value
            ))
        }
    }

    private fun restartService() {
        // For simplicity, we just update an intent extra; service restart logic can be triggered from activity.
        // In production, send an intent to the service.
    }
}