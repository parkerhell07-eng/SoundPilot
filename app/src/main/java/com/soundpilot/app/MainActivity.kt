package com.soundpilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundpilot.app.data.ThemeMode
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.ui.navigation.SoundPilotApp
import com.soundpilot.app.ui.permission.PermissionGate
import com.soundpilot.app.ui.player.PlaybackViewModel
import com.soundpilot.app.ui.theme.SoundPilotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by ServiceLocator.settingsRepository.themeMode
                .collectAsStateWithLifecycle(ThemeMode.DARK)

            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                )
            }

            SoundPilotTheme(themeMode) {
                PermissionGate {
                    val playbackVm: PlaybackViewModel = viewModel()
                    SoundPilotApp(playbackVm = playbackVm)
                }
            }
        }
    }
}
