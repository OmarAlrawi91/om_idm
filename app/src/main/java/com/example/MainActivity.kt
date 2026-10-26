package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DownloadViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent if shared from browser
        handleIncomingIntent(intent)

        setContent {
            val darkPreset by viewModel.darkThemePreset.collectAsStateWithLifecycle()

            MyApplicationTheme(preset = darkPreset, accentColor = darkPreset.accent) {
                // Request storage and notification permissions
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions result handled */ }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            val uriString = data.toString()
            if (uriString.startsWith("magnet:?")) {
                viewModel.addTorrent(uriString, true)
            } else if (uriString.startsWith("http")) {
                viewModel.showFormatModal(uriString)
            }
        } else if (Intent.ACTION_SEND == action && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val clean = sharedText.trim()
                if (clean.startsWith("magnet:?")) {
                    viewModel.addTorrent(clean, true)
                } else if (clean.startsWith("http")) {
                    viewModel.showFormatModal(clean)
                }
            }
        }
    }
}
