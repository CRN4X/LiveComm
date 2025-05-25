package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler

@Composable
fun RoleSelectionScreen(userName: String, onBack: () -> Unit, onRoleSelected: (String) -> Unit, onClose: () -> Unit) {
    BackHandler {
        onBack()
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
        Text("Select your role:")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRoleSelected("tx") }, modifier = Modifier.fillMaxWidth()) {
            Text("Transmitter (Tx)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onRoleSelected("rx") }, modifier = Modifier.fillMaxWidth()) {
            Text("Receiver (Rx)")
        }
        Spacer(modifier = Modifier.height(530.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close App")
        }
    }
}
