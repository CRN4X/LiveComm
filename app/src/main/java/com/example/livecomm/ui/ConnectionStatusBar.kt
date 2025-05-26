package com.example.livecomm.ui

import androidx.camera.core.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import java.net.Socket
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape


@Composable
fun ConnectionStatusBar(
    socket: Socket?,
    modifier: Modifier = Modifier
) {
    val isConnected = remember(socket) {
        socket?.isConnected == true && !socket.isClosed
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (isConnected) "Connection: Active" else "Connection: Inactive",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
