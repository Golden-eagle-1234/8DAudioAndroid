package com.pirate.audio8d

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AudioProcessorService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var audioManager: AudioManager
    private var captureManager: AudioCaptureManager? = null
    private var playerManager: AudioPlayerManager? = null
    private var serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        const val CHANNEL_ID = "audio_8d_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.pirate.audio8d.STOP"
        private const val TAG = "AudioSvc"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "8D Pirate::Audio")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)

        // Load native library early
        if (!NativeBridge.ensureLoaded()) {
            stopSelf()
            return
        }

        // Register headphone plug/unplug receiver for toast
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headsetReceiver, filter)

        // Request battery optimisation ignore
        requestBatteryOptimisation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopProcessing()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Retrieve initial mode from intent extras or default to system‑wide
        val mode = intent?.getStringExtra("MODE") ?: "system"
        scope.launch {
            startAudioProcessing(mode)
        }
        return START_STICKY
    }

    private suspend fun startAudioProcessing(mode: String) {
        // Stop any previous processing
        stopProcessing()

        val sampleRate = 48000
        val framesPerBurst = 1024
        if (!NativeBridge.initEngine(sampleRate, framesPerBurst)) {
            Log.e(TAG, "Engine init failed")
            stopSelf()
            return
        }

        when (mode) {
            "system" -> {
                captureManager = AudioCaptureManager(
                    this,
                    onData = { buf, frames -> NativeBridge.processCapture(buf, frames) },
                    onError = { msg ->
                        Log.e(TAG, "Capture error: $msg")
                        showToast("Capture failed, switching to player mode")
                        scope.launch { switchToPlayerMode() }
                    }
                )
                val success = captureManager?.startCapture() ?: false
                if (!success) {
                    // Try Shizuku then fallback
                    if (ShizukuHelper.isAvailable()) {
                        ShizukuHelper.requestPermission()
                        delay(1000)
                        if (captureManager?.startCapture() != true) {
                            switchToPlayerMode()
                        }
                    } else {
                        switchToPlayerMode()
                    }
                }
            }
            "player" -> {
                switchToPlayerMode()
            }
        }
    }

    private suspend fun switchToPlayerMode() {
        playerManager = AudioPlayerManager { msg ->
            Log.e(TAG, "Player error: $msg")
        }
        playerManager?.start()
    }

    private fun stopProcessing() {
        captureManager?.stopCapture()
        playerManager?.stop()
        NativeBridge.destroyEngine()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)
        stopProcessing()
        wakeLock.release()
        serviceJob.cancel()
        super.onDestroy()
    }

    // --- Headphone detection toast ---
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                val message = if (state == 1) "Headphones connected" else "Headphones disconnected"
                showToast(message)
            }
        }
    }

    private fun showToast(message: String) {
        // Use a simple coroutine to post on main thread if needed
        scope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(this@AudioProcessorService, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // --- Notification ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "8D Pirate Processor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that audio processing is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AudioProcessorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("8D Pirate Processor")
            .setContentText("Audio processing running...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun requestBatteryOptimisation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }
}