package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun PairingScreen(userName: String, role: String, onPaired: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var paired by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Welcome $userName!!",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("Enter Security Code:")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = code,
            onValueChange = { code = it },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // TODO: Add real pairing logic here
                paired = true
                deviceName = if (role == "tx") "Receiver Device" else "Transmitter Device"
                onPaired(deviceName)
            },
            enabled = code.isNotBlank()
        ) {
            Text("Pair")
        }
        if (paired) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pairing successful with $deviceName!")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ensure both devices are on the same WiFi network.")
    }
}
