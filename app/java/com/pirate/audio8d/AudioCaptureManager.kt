package com.pirate.audio8d

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioCaptureManager(
    private val context: Context,
    private val onData: (ByteBuffer, Int) -> Unit,
    private val onError: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var isCapturing = false

    companion object {
        private const val TAG = "AudioCaptureMgr"
        const val SAMPLE_RATE = 48000
        const val CHANNEL_COUNT = 2
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FRAMES = 1024
    }

    @SuppressLint("MissingPermission")
    suspend fun startCapture(): Boolean {
        if (isCapturing) return true

        return try {
            val config = AudioPlaybackCaptureConfiguration.Builder(context)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val minBufBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AUDIO_FORMAT)
            val bufferBytes = maxOf(minBufBytes, BUFFER_SIZE_FRAMES * CHANNEL_COUNT * 2)

            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build())
                .setBufferSizeInBytes(bufferBytes)
                .build()

            audioRecord?.startRecording()
            isCapturing = true

            captureJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteBuffer.allocateDirect(bufferBytes).order(ByteOrder.nativeOrder())
                while (isActive && isCapturing) {
                    val read = audioRecord?.read(buffer, bufferBytes) ?: -1
                    if (read > 0) {
                        buffer.rewind()
                        onData(buffer, read / (2 * CHANNEL_COUNT)) // frames
                    } else if (read < 0) {
                        onError("AudioRecord read error: $read")
                        break
                    }
                }
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Capture permission denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            false
        }
    }

    fun stopCapture() {
        isCapturing = false
        captureJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}