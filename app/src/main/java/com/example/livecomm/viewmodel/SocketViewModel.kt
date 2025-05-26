package com.example.livecomm.viewmodel

import androidx.lifecycle.ViewModel
import java.net.ServerSocket
import java.net.Socket

class SocketViewModel : ViewModel() {
    var serverSocket: ServerSocket? = null
    var clientSocket: Socket? = null
    var socket: Socket? = null

    override fun onCleared() {
        super.onCleared()
        clientSocket?.close()
        serverSocket?.close()
    }
}
