package com.example.livecomm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select your role:")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRoleSelected("tx") }, modifier = Modifier.fillMaxWidth()) {
            Text("Transmitter (Tx)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onRoleSelected("rx") }, modifier = Modifier.fillMaxWidth()) {
            Text("Receiver (Rx)")
        }
    }
}

