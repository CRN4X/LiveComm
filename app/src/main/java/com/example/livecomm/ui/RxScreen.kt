package com.example.livecomm.ui

import timber.log.Timber
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.net.InetSocketAddress
import kotlinx.coroutines.launch
//import com.example.livecomm.model.AppState
import com.example.livecomm.viewmodel.SocketViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RxScreen(
    userName: String,
    onBack: () -> Unit,
    pairedDeviceName: String,
    onClose: () -> Unit,
    onConnected: (String) -> Unit,
    socketViewModel: SocketViewModel = viewModel()
) {
    BackHandler { onBack() }

    var listening by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Not connected") }
    var socket by remember { mutableStateOf<Socket?>(null) }
    var scannedIp by remember { mutableStateOf<String?>(null) }
    var scannedPort by remember { mutableStateOf<String?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val parts = result.contents.split(":")
            if (parts.size == 2) {
                scannedIp = parts[0]
                scannedPort = parts[1]
                Timber.tag("RxScreen").d("Scanned QR code: IP=$scannedIp, Port=$scannedPort")
            } else {
                Timber.tag("RxScreen").e("Invalid QR code format: ${result.contents}")
                connectionError = "Invalid QR code format. Expected IP:PORT"
            }
        }
    }

    // Function to attempt connection
    val connectToTransmitter = {
        if (scannedIp != null && scannedPort != null) {
            println("Will connect to IP: $scannedIp, Port: $scannedPort")
            coroutineScope.launch {
                connectionStatus = "Connecting..."
                connectionError = null

                withContext(Dispatchers.IO) {
                    try {
                        Timber.tag("RxScreen").d("Attempting to connect to $scannedIp:$scannedPort")

                        // Create socket with timeout
                        val newSocket = Socket()  // AppState.socket //
                        newSocket.connect(InetSocketAddress(scannedIp, scannedPort!!.toInt()), 5000) // 5 second timeout
                        socket = newSocket

                        Timber.tag("RxScreen").d("Socket connected successfully")
                        socketViewModel.socket = socket

                        // Send your user name
                        newSocket.getOutputStream().write((userName + "\n").toByteArray())
                        newSocket.getOutputStream().flush()

                        Timber.tag("RxScreen").d("Sent username: $userName")
                        // Read the other device's user name
                        val otherDeviceName = newSocket.getInputStream().bufferedReader().readLine()
                        Timber.tag("RxScreen").d("Received other device name: $otherDeviceName")

                        withContext(Dispatchers.Main) {
                            connectionStatus = "Transmitter connected!"
                            onConnected(otherDeviceName)
                        }
                    } catch (e: Exception) {
                        Timber.tag("RxScreen").e(e, "Connection error")
                        val errorMsg = e.message ?: "Unknown error"
                        withContext(Dispatchers.Main) {
                            connectionStatus = "Connection failed"
                            connectionError = "Error: $errorMsg"
                            // Reset listening state
                            listening = false
                        }
                    }
                }
            }
        } else {
            connectionError = "IP address or port is missing"
        }
    }

    // Connect when listening is activated
    LaunchedEffect(listening) {
        if (listening) {
            connectToTransmitter()
        } else {
            // Clean up socket
            withContext(Dispatchers.IO) {
                try {
                    //socket?.close()
                } catch (e: Exception) {
                    Timber.tag("RxScreen").e(e, "Error closing socket")
                }
                //socket = null
            }
            connectionStatus = "Not connected"
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
            onClick = {
                val options = ScanOptions()
                options.setOrientationLocked(true)
                scanLauncher.launch(options)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code")
        }

        if (scannedIp != null && scannedPort != null) {
            Text("Scanned IP: $scannedIp, Port: $scannedPort")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Status: $connectionStatus")

            connectionError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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
