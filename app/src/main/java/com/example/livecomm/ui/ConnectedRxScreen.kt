package com.example.livecomm.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.net.Socket

@Composable
fun ConnectedRxScreen(
    userName: String,
    otherDeviceName: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    socket: Socket?
) {
    BackHandler { onBack() }

    //val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var receivedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf("Checking connection...") }
    var connectionActive by remember { mutableStateOf(false) }
    var isReceiving by remember { mutableStateOf(false) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var fps by remember { mutableIntStateOf(0) }

    // Verify connection
    LaunchedEffect(socket) {
        connectionStatus = "Checking connection..."
        connectionActive = false

        if (socket == null) {
            connectionStatus = "No connection"
            return@LaunchedEffect
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (!socket.isClosed && socket.isConnected) {
                    // Listen for ping and respond with pong
                    try {
                        withTimeoutOrNull(5000) {
                            val input = socket.getInputStream().bufferedReader().readLine()
                            if (input == "PING") {
                                socket.getOutputStream().write("PONG\n".toByteArray())
                                socket.getOutputStream().flush()

                                withContext(Dispatchers.Main) {
                                    connectionStatus = "Connected to $otherDeviceName"
                                    connectionActive = true
                                }
                                return@withTimeoutOrNull true
                            }
                            return@withTimeoutOrNull false
                        } ?: withContext(Dispatchers.Main) {
                            connectionStatus = "Connection timeout"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            connectionStatus = "Connection error: ${e.message}"
                            Timber.tag("ConnectedRxScreen").e(e, "Connection verification failed")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        connectionStatus = "Socket is closed or not connected"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    connectionStatus = "Error checking connection"
                    Timber.tag("ConnectedRxScreen").e(e, "Socket error")
                }
            }
        }
    }

    // Start receiving video frames
    LaunchedEffect(socket, isReceiving) {
        if (socket == null || !isReceiving) return@LaunchedEffect

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream = socket.getInputStream()
                val sizeBuffer = ByteArray(4)

                while (isActive && socket.isConnected && !socket.isClosed && isReceiving) {
                    try {
                        // Read frame size (4 bytes)
                        val bytesRead = inputStream.read(sizeBuffer, 0, 4)
                        if (bytesRead != 4) {
                            Timber.tag("ConnectedRxScreen")
                                .d("End of stream or incomplete size data")
                            break
                        }

                        val frameSize = ((sizeBuffer[0].toInt() and 0xFF) shl 24) or
                                ((sizeBuffer[1].toInt() and 0xFF) shl 16) or
                                ((sizeBuffer[2].toInt() and 0xFF) shl 8) or
                                (sizeBuffer[3].toInt() and 0xFF)

                        if (frameSize <= 0 || frameSize > 10_000_000) { // Sanity check
                            Timber.tag("ConnectedRxScreen").w("Invalid frame size: $frameSize")
                            continue
                        }

                        // Read frame data
                        val frameData = ByteArray(frameSize)
                        var totalBytesRead = 0

                        while (totalBytesRead < frameSize) {
                            val bytesRead = inputStream.read(
                                frameData,
                                totalBytesRead,
                                frameSize - totalBytesRead
                            )

                            if (bytesRead <= 0) {
                                Timber.tag("ConnectedRxScreen").d("End of stream during frame data")
                                break
                            }
                            totalBytesRead += bytesRead
                        }

                        if (totalBytesRead == frameSize) {
                            // Decode JPEG to bitmap
                            val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameSize)

                            // Update UI on main thread
                            withContext(Dispatchers.Main) {
                                receivedBitmap = bitmap

                                // Calculate FPS
                                val currentTime = System.currentTimeMillis()
                                frameCount++

                                if (currentTime - lastFrameTime >= 1000) {
                                    fps = frameCount
                                    frameCount = 0
                                    lastFrameTime = currentTime
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("ConnectedRxScreen").e(e, "Error receiving frame")
                        withContext(Dispatchers.Main) {
                            errorMessage = "Frame error: ${e.message}"
                        }
                        delay(1000) // Wait before retrying
                    }
                }
            } catch (e: Exception) {
                Timber.tag("ConnectedRxScreen").e(e, "Error in video reception")
                withContext(Dispatchers.Main) {
                    errorMessage = "Reception error: ${e.message}"
                    isReceiving = false
                }
            }
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
        onDispose {
            isReceiving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "Dear $userName,",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Connection info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (connectionActive) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = connectionStatus,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Error message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Video display area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (isReceiving && receivedBitmap != null) {
                // Display received video
                Image(
                    bitmap = receivedBitmap!!.asImageBitmap(),
                    contentDescription = "Video stream",
                    modifier = Modifier.fillMaxSize()
                )

                // FPS counter and LIVE indicator
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    // FPS counter
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$fps FPS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // LIVE indicator
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                // Placeholder when not receiving
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (!connectionActive)
                            "Waiting for connection..."
                        else if (!isReceiving)
                            "Press Start to receive video"
                        else
                            "Waiting for video stream...",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streaming control button
        Button(
            onClick = { isReceiving = !isReceiving },
            modifier = Modifier.fillMaxWidth(),
            enabled = connectionActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReceiving)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isReceiving) "Stop Receiving" else "Start Receiving")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Close button
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Close App")
        }
    }
}