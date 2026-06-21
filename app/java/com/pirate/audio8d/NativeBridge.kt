package com.pirate.audio8d

import android.util.Log
import java.nio.ByteBuffer

/**
 * Singleton bridge to native DSP engine.
 * All JNI field/method IDs are cached in JNI_OnLoad.
 */
object NativeBridge {
    private const val TAG = "NativeBridge"
    private var nativeLoaded = false

    /**
     * Must be called once. Called from Application or Service onCreate.
     */
    fun ensureLoaded(): Boolean {
        if (nativeLoaded) return true
        try {
            System.loadLibrary("audio8d")
            nativeLoaded = true
            Log.i(TAG, "Native library loaded successfully.")
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
            return false
        }
    }

    // --- Parameter setters ---
    external fun setOrbitSpeed(speedHz: Float)      // 0.05 - 0.8
    external fun setDepth(depth: Float)              // 0.0 - 1.0
    external fun setReverbMix(mix: Float)            // 0.0 - 1.0
    external fun setDelayMix(mix: Float)             // 0.0 - 1.0
    external fun setBassBoost(gainDB: Float)         // 0.0 - 12.0

    // --- Lifecycle ---
    /**
     * Initialise the engine with device sample rate and buffer size.
     * @return true on success.
     */
    external fun initEngine(sampleRate: Int, framesPerBurst: Int): Boolean
    external fun destroyEngine()

    /**
     * Start / stop the Oboe playback stream.
     */
    external fun startPlayback(): Boolean
    external fun stopPlayback()

    /**
     * Process a chunk of captured audio (stereo interleaved short) and write into output ring.
     * The output will be consumed by Oboe callback.
     * @param inputShortBuffer direct ByteBuffer (as short array, little‑endian)
     * @param numFrames number of stereo frames
     */
    external fun processCapture(inputShortBuffer: ByteBuffer, numFrames: Int)

    /**
     * Write a test tone (generated in native) into output ring for player mode.
     * Call continuously from a thread.
     * @param numFrames number of stereo frames to generate and enqueue
     */
    external fun generateTestTone(numFrames: Int)
}