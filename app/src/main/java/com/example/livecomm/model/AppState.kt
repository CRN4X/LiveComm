package com.example.livecomm.model

import java.net.ServerSocket
import java.net.Socket

data class AppState(
    val screen: String = "onboarding",
    val userName: String = "",
    val role: String = "",
    val pairedDeviceName: String = "",
    val otherDeviceName: String = "",
    val defaultPort: Int = 4444,
    val socket: Socket? = null,        // Socket for RX device
    val clientSocket: Socket? = null,  // Client socket for TX device
    val serverSocket: ServerSocket? = null  // Server socket for TX device
)
