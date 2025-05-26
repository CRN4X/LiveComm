package com.example.livecomm.ui

import android.Manifest
import android.content.pm.PackageManager
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.util.concurrent.Executors
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn

@OptIn(ExperimentalGetImage::class)
@Composable
fun ConnectedTxScreen(
    userName: String,
    otherDeviceName: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    socket: Socket?
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var isStreaming by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf("Checking connection...") }
    var connectionActive by remember { mutableStateOf(false) }

    // Camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Check camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            errorMessage = "Camera permission is required for streaming"
        }
    }

    // Request camera permission on first composition
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, permission)

        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

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
                    // Try to send a ping to verify connection
                    try {
                        socket.getOutputStream().write("PING\n".toByteArray())
                        socket.getOutputStream().flush()

                        // Wait for response with timeout
                        withTimeoutOrNull(3000) {
                            val response = socket.getInputStream().bufferedReader().readLine()
                            if (response == "PONG") {
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
                            Timber.tag("ConnectedTxScreen").e(e, "Connection verification failed")
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
                    Timber.tag("ConnectedTxScreen").e(e, "Socket error")
                }
            }
        }
    }

    // Clean up resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            isStreaming = false
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

        // Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (hasCameraPermission && isStreaming) {
                // Camera preview with streaming
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        // Set up camera
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview use case
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            // Image analysis for streaming
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val image = imageProxy.image

                                if (image != null && socket?.isConnected == true && !socket.isClosed && isStreaming) {
                                    try {
                                        // Convert YUV to JPEG
                                        val yBuffer = image.planes[0].buffer
                                        val uBuffer = image.planes[1].buffer
                                        val vBuffer = image.planes[2].buffer

                                        val ySize = yBuffer.remaining()
                                        val uSize = uBuffer.remaining()
                                        val vSize = vBuffer.remaining()

                                        val nv21 = ByteArray(ySize + uSize + vSize)

                                        yBuffer.get(nv21, 0, ySize)
                                        vBuffer.get(nv21, ySize, vSize)
                                        uBuffer.get(nv21, ySize + vSize, uSize)

                                        val yuvImage = YuvImage(
                                            nv21,
                                            ImageFormat.NV21,
                                            image.width,
                                            image.height,
                                            null
                                        )

                                        val jpegStream = ByteArrayOutputStream()
                                        yuvImage.compressToJpeg(
                                            Rect(0, 0, image.width, image.height),
                                            60, // Lower quality for better performance
                                            jpegStream
                                        )

                                        val jpegBytes = jpegStream.toByteArray()

                                        // Send frame size and data
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val outputStream = socket.getOutputStream()

                                                // Send frame size (4 bytes)
                                                val frameSize = jpegBytes.size
                                                val sizeBytes = ByteArray(4)
                                                sizeBytes[0] = (frameSize shr 24).toByte()
                                                sizeBytes[1] = (frameSize shr 16).toByte()
                                                sizeBytes[2] = (frameSize shr 8).toByte()
                                                sizeBytes[3] = frameSize.toByte()

                                                outputStream.write(sizeBytes)
                                                outputStream.write(jpegBytes)
                                                outputStream.flush()
                                            } catch (e: Exception) {
                                                Timber.tag("ConnectedTxScreen")
                                                    .e(e, "Error sending video")
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Streaming error: ${e.message}"
                                                    isStreaming = false
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Timber.tag("ConnectedTxScreen")
                                            .e(e, "Error processing image")
                                    }
                                }

                                imageProxy.close()
                            }

                            try {
                                // Unbind any bound use cases
                                cameraProvider.unbindAll()

                                // Bind use cases to camera
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Timber.tag("ConnectedTxScreen").e(e, "Use case binding failed")
                                errorMessage = "Camera error: ${e.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Live indicator
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Placeholder when not streaming
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (!hasCameraPermission)
                            "Camera permission required"
                        else
                            "Press Start to begin streaming",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streaming control button
        Button(
            onClick = { isStreaming = !isStreaming },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasCameraPermission && connectionActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isStreaming)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isStreaming) "Stop Streaming" else "Start Streaming")
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