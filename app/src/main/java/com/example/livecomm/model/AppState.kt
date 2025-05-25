package com.example.livecomm.model

data class AppState(
    val screen: String = "onboarding",
    val userName: String = "",
    val role: String = "",
    val pairedDeviceName: String = "",
    val otherDeviceName: String = "",
    val defaultPort: Int = 4444
)
