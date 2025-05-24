package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import androidx.activity.compose.rememberLauncherForActivityResult

@Composable
fun RxScreen(userName: String, pairedDeviceName: String, onClose: () -> Unit) {
    var listening by remember { mutableStateOf(false) }
    var scannedIp by remember { mutableStateOf<String?>(null) }
    var scannedPort by remember { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val parts = result.contents.split(":")
            if (parts.size == 2) {
                scannedIp = parts[0]
                scannedPort = parts[1]
            }
        }
    }

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

        Button(
            onClick = { scanLauncher.launch(ScanOptions()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code")
        }

        if (scannedIp != null && scannedPort != null) {
            Text("Scanned IP: $scannedIp, Port: $scannedPort")
            Spacer(modifier = Modifier.height(16.dp))
            if (!listening) {
                Button(
                    onClick = { listening = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Listening")
                }
            } else {
                Text("PLAYING VIDEO...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { listening = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("STOP")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Please scan a QR code to get IP and port.")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close App")
        }
    }
}
