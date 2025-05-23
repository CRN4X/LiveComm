package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RxScreen(pairedDeviceName: String, onClose: () -> Unit) {
    var listening by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connected to device: $pairedDeviceName")
        Spacer(modifier = Modifier.height(16.dp))
        if (!listening) {
            Button(onClick = { listening = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Start Listening")
            }
        } else {
            Text("PLAYING VIDEO...")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { listening = false }, modifier = Modifier.fillMaxWidth()) {
                Text("STOP")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close App")
        }
    }
}

