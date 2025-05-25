package com.example.livecomm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.livecomm.model.AppState
import com.example.livecomm.ui.*
import kotlinx.coroutines.launch //import com.example.livecomm.UserPreferences
import java.net.NetworkInterface
import java.net.Inet4Address
//import androidx.compose.foundation.layout.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localIp = getLocalIpAddress() ?: "0.0.0.0"

        setContent {
            var appState by remember { mutableStateOf(AppState()) }
            var isNameLoaded by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            // Load saved name from DataStore when the app starts
            LaunchedEffect(Unit) {
                val savedName = UserPreferences.getUserName(this@MainActivity)
                if (!savedName.isNullOrBlank()) {
                    appState = appState.copy(screen = "role", userName = savedName)
                }
                isNameLoaded = true
            }

            if (!isNameLoaded) {
                LoadingScreen()
            } else {
                when (appState.screen) {
                    "onboarding" -> OnboardingScreen(
                        onNameEntered = { name ->

                            // Save name to DataStore
                            coroutineScope.launch {
                                UserPreferences.saveUserName(this@MainActivity, name)
                            }
                            appState = appState.copy(userName = name, screen = "role")
                        }
                    )
                    "role" -> RoleSelectionScreen(
                        userName = appState.userName,
                        onBack = { appState = appState.copy(screen = "onboarding") },
                        onRoleSelected = { role ->
                            appState = appState.copy(
                                role = role,
                                screen = if (role == "tx") "tx" else "rx"
                            )
                        },
                        onClose = { finish() }
                    )
                    "tx" -> TxScreen(
                        ip = localIp,
                        port = appState.defaultPort,
                        userName = appState.userName,
                        onBack = { appState = appState.copy(screen = "role") },
                        onClose = { finish() },
                        onConnected = { otherDeviceName ->
                            appState = appState.copy(
                                screen = "connected",
                                otherDeviceName = otherDeviceName,
                            )
                        }
                    )
                    "rx" -> RxScreen(
                        userName = appState.userName,
                        onBack = { appState = appState.copy(screen = "role") },
                        pairedDeviceName = appState.pairedDeviceName,
                        onClose = { finish() },
                        onConnected = { otherDeviceName ->
                            appState = appState.copy(
                                screen = "connected",
                                otherDeviceName = otherDeviceName
                            )
                        }
                    )
                    "connected" -> ConnectedScreen(
                        userName = appState.userName,
                        onBack = {appState = appState.copy(screen = "role")},
                        otherDeviceName = appState.otherDeviceName,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

fun getLocalIpAddress(): String? {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        val addrs = intf.inetAddresses
        for (addr in addrs) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                return addr.hostAddress
            }
        }
    }
    return null
}
