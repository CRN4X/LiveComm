package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun TxScreen(userName: String, pairedDeviceName: String, onClose: () -> Unit) {
    var streaming by remember { mutableStateOf(false) }
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
        Text("Connected to device: $pairedDeviceName")
        Spacer(modifier = Modifier.height(16.dp))
        if (!streaming) {
            Button(onClick = { streaming = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Start Tx")
            }
        } else {
            Text("VIDEO STREAMING...")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { streaming = false }, modifier = Modifier.fillMaxWidth()) {
                Text("STOP")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close App")
        }
    }
}

