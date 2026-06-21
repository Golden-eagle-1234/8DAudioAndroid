package com.pirate.audio8d

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pirate.audio8d.ui.screens.MainScreen
import com.pirate.audio8d.ui.theme.PirateTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private var serviceBound = false
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startService()
        } else {
            Toast.makeText(this, "Permissions required for audio capture", Toast.LENGTH_LONG).show()
            // Still allow player mode
            startServiceWithMode("player")
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { serviceBound = true }
        override fun onServiceDisconnected(name: ComponentName?) { serviceBound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Shizuku permission handling
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            ShizukuHelper.onRequestPermissionResult(requestCode, grantResult)
        }

        setContent {
            PirateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        } else {
            // All permissions granted, start service if not already
            startService()
        }
    }

    private fun startService() {
        val intent = Intent(this, AudioProcessorService::class.java).apply {
            putExtra("MODE", viewModel.mode.value)
        }
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startServiceWithMode(mode: String) {
        viewModel.setMode(mode)
        startService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) unbindService(serviceConnection)
    }
}