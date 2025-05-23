package com.example.livecomm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.livecomm.model.AppState
import com.example.livecomm.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var appState by remember { mutableStateOf(AppState()) }

            when (appState.screen) {
                "onboarding" -> OnboardingScreen(
                    onNameEntered = { name ->
                        appState = appState.copy(userName = name, screen = "role")
                    }
                )
                "role" -> RoleSelectionScreen(
                    onRoleSelected = { role ->
                        appState = appState.copy(role = role, screen = "pairing")
                    }
                )
                "pairing" -> PairingScreen(
                    role = appState.role,
                    onPaired = { deviceName ->
                        appState = appState.copy(pairedDeviceName = deviceName, screen = if (appState.role == "tx") "tx" else "rx")
                    }
                )
                "tx" -> TxScreen(
                    pairedDeviceName = appState.pairedDeviceName,
                    onClose = { finish() }
                )
                "rx" -> RxScreen(
                    pairedDeviceName = appState.pairedDeviceName,
                    onClose = { finish() }
                )
            }
        }
    }
}
