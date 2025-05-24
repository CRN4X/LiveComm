package com.example.livecomm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun TxScreen(
    ip: String,
    port: Int,
    userName: String,
    pairedDeviceName: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val qrContent = "$ip:$port" // Or use JSON for more info
    val qrBitmap = remember(qrContent) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 400, 400)
        } catch (e: Exception) {
            null
        }
    }

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
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connected to device: $pairedDeviceName")
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(24.dp))
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
