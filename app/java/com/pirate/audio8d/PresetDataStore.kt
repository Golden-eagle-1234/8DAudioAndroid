package com.pirate.audio8d

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "8d_presets")

data class AudioPreset(
    val name: String,
    val orbitSpeed: Float,
    val depth: Float,
    val reverbMix: Float,
    val delayMix: Float,
    val bassBoost: Float
)

object PresetKeys {
    val ORBIT_SPEED = floatPreferencesKey("orbit_speed")
    val DEPTH = floatPreferencesKey("depth")
    val REVERB_MIX = floatPreferencesKey("reverb_mix")
    val DELAY_MIX = floatPreferencesKey("delay_mix")
    val BASS_BOOST = floatPreferencesKey("bass_boost")
    val LAST_PRESET_NAME = stringPreferencesKey("last_preset")
}

class PresetDataStore(private val context: Context) {

    val currentSettings: Flow<AudioPreset> = context.dataStore.data.map { prefs ->
        AudioPreset(
            name = prefs[PresetKeys.LAST_PRESET_NAME] ?: "Default",
            orbitSpeed = prefs[PresetKeys.ORBIT_SPEED] ?: 0.15f,
            depth = prefs[PresetKeys.DEPTH] ?: 0.8f,
            reverbMix = prefs[PresetKeys.REVERB_MIX] ?: 0.4f,
            delayMix = prefs[PresetKeys.DELAY_MIX] ?: 0.2f,
            bassBoost = prefs[PresetKeys.BASS_BOOST] ?: 4.0f
        )
    }

    suspend fun savePreset(name: String, preset: AudioPreset) {
        context.dataStore.edit { prefs ->
            prefs[PresetKeys.LAST_PRESET_NAME] = name
            prefs[PresetKeys.ORBIT_SPEED] = preset.orbitSpeed
            prefs[PresetKeys.DEPTH] = preset.depth
            prefs[PresetKeys.REVERB_MIX] = preset.reverbMix
            prefs[PresetKeys.DELAY_MIX] = preset.delayMix
            prefs[PresetKeys.BASS_BOOST] = preset.bassBoost
        }
    }
}