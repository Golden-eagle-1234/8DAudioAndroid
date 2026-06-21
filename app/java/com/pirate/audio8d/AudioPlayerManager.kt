package com.pirate.audio8d

import android.util.Log
import kotlinx.coroutines.*

/**
 * Player mode: drives the native engine to generate continuous test tone.
 */
class AudioPlayerManager(
    private val onError: (String) -> Unit
) {
    private var playerJob: Job? = null
    private var isPlaying = false

    companion object {
        private const val TAG = "AudioPlayerMgr"
        private const val FRAMES_PER_BURST = 1024
    }

    suspend fun start() {
        if (isPlaying) return
        if (!NativeBridge.startPlayback()) {
            onError("Failed to start playback engine")
            return
        }
        isPlaying = true
        playerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isPlaying) {
                NativeBridge.generateTestTone(FRAMES_PER_BURST)
                // Small sleep to avoid busy‑looping; Oboe callback will consume.
                delay(5)
            }
        }
        Log.i(TAG, "Player mode started.")
    }

    fun stop() {
        isPlaying = false
        playerJob?.cancel()
        NativeBridge.stopPlayback()
        Log.i(TAG, "Player mode stopped.")
    }
}