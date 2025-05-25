package com.example.livecomm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun ConnectedScreen(userName: String, onBack: () -> Unit, otherDeviceName: String, onClose: () -> Unit) {

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Dear $userName,",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "You are successfully connected with $otherDeviceName!!",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) {
            Text("Close App")
        }
    }
}
