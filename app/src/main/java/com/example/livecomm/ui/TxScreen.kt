package com.example.livecomm.ui

//import android.view.Display
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
// import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import androidx.activity.compose.BackHandler
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
//import android.util.Log
import timber.log.Timber
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import com.example.livecomm.viewmodel.SocketViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TxScreen(
    ip: String,
    port: Int,
    userName: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onConnected: (String) -> Unit,
    socketViewModel: SocketViewModel = viewModel()
) {
    BackHandler {
        onBack()
    }

    val qrContent = "$ip:$port"
    val qrBitmap = remember(qrContent) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400)
        } catch (e: Exception) {
            Timber.tag("TxScreen").d(e, "Failed to generate QR code")
            null
        }
    }

    var streaming by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Waiting for receiver...") }
    var serverError by remember { mutableStateOf<String?>(null) }

    // Server socket state
    var serverSocket by remember { mutableStateOf<ServerSocket?>(null) }
    var clientSocket by remember { mutableStateOf<Socket?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Start server when streaming starts
    LaunchedEffect(streaming) {
        Timber.tag("TxScreen").e("Streaming state changed: $streaming")

        if (streaming) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    Timber.tag("TxScreen").d("Creating server socket on port $port")

                    // Close any existing server socket
                    serverSocket?.close()

                    // Create new server socket
                    val server = ServerSocket()
                    server.reuseAddress = true
                    server.bind(InetSocketAddress(port))
                    serverSocket = server

                    socketViewModel.serverSocket = serverSocket

                    Timber.tag("TxScreen").d("Server socket created and bound to port $port")
                    withContext(Dispatchers.Main) {
                        connectionStatus = "Waiting for the connection...." //"Listening on port $port..."
                        serverError = null
                    }

                    Timber.tag("TxScreen").d("Waiting for client connection...")
                    val client = server.accept()
                    clientSocket = client

                    socketViewModel.clientSocket = clientSocket

                    Timber.tag("TxScreen")
                        .d("Client connected from ${client.inetAddress.hostAddress}")

                    // Send your user name
                    client.getOutputStream().write((userName + "\n").toByteArray())
                    client.getOutputStream().flush()

                    Timber.tag("TxScreen").d("Sent username: $userName")

                    // Read the other device's user name
                    val otherDeviceName = client.getInputStream().bufferedReader().readLine()
                    Timber.tag("TxScreen").d("Received other device name: $otherDeviceName")

                    withContext(Dispatchers.Main) {
                        connectionStatus = "Receiver connected!"
                        onConnected(otherDeviceName)
                    }
                } catch (e: Exception) {
                    Timber.tag("TxScreen").e(e, "Server error")
                    withContext(Dispatchers.Main) {
                        connectionStatus = "Error: Connection failed"
                        serverError = e.message
                        streaming = false // Reset streaming state on error
                    }
                }
            }
        } else {
            // Clean up sockets
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    Timber.tag("TxScreen").d("Closing sockets")
                    clientSocket?.close()
                    serverSocket?.close()
                } catch (e: Exception) {
                    Timber.tag("TxScreen").e(e, "Error closing sockets")
                }
                clientSocket = null
                serverSocket = null

                withContext(Dispatchers.Main) {
                    connectionStatus = "Waiting for receiver..."
                }
            }
        }
    }

    // Clean up when component is removed
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    clientSocket?.close()
                    serverSocket?.close()
                } catch (e: Exception) {
                    Timber.tag("TxScreen").e(e, "Error closing sockets on dispose")
                }
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

//        Spacer(modifier = Modifier.height(16.dp))
//        Text("Connected to device: $pairedDeviceName")
//
//        Spacer(modifier = Modifier.height(16.dp))
//        Text("Your IP address: $ip")
//        Text("Port: $port")

        Spacer(modifier = Modifier.height(8.dp))
        Text("Scan this QR code on the receiver device to connect:")

        Spacer(modifier = Modifier.height(8.dp))
        qrBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Status: $connectionStatus")

        serverError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Error: $it",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!streaming) {
            Button(
                onClick = { streaming = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Tx")
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { streaming = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("STOP")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close App")
        }
    }
}
