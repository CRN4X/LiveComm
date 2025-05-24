package com.example.livecomm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.livecomm.model.AppState
import com.example.livecomm.ui.*
import kotlinx.coroutines.launch //import com.example.livecomm.UserPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    var initialScreen = "onboarding"
    var savedName: String? = null

    // Check for saved name synchronously before Compose starts
    lifecycleScope.launch {
        savedName = UserPreferences.getUserName(this@MainActivity)
        setContent {
                var appState by remember {
                    mutableStateOf(
                        if (!savedName.isNullOrBlank())
                            AppState(screen = "role", userName = savedName!!)
                        else
                            AppState()
                    )
                }

                when (appState.screen) {
                    "onboarding" -> OnboardingScreen(
                        onNameEntered = { name ->
                            // Save name to DataStore
                            lifecycleScope.launch {
                                UserPreferences.saveUserName(this@MainActivity, name)
                            }
                            appState = appState.copy(userName = name, screen = "role")
                        }
                    )

                    "role" -> RoleSelectionScreen(
                        userName = appState.userName, // Pass the name
                        onRoleSelected = { role ->
                            appState = appState.copy(role = role, screen = "pairing")
                        }
                    )

                    "pairing" -> PairingScreen(
                        userName = appState.userName, // Pass the name
                        role = appState.role,
                        onPaired = { deviceName ->
                            appState = appState.copy(
                                pairedDeviceName = deviceName,
                                screen = if (appState.role == "tx") "tx" else "rx"
                            )
                        }
                    )

                    "tx" -> TxScreen(
                        userName = appState.userName, // Pass the name
                        pairedDeviceName = appState.pairedDeviceName,
                        onClose = { finish() }
                    )

                    "rx" -> RxScreen(
                        userName = appState.userName, // Pass the name
                        pairedDeviceName = appState.pairedDeviceName,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}
