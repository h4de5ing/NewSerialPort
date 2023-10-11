package com.android.serialport2.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WSViewModel : ViewModel() {
    private var client: WebSocketClient? = null
    private val _uiState = MutableStateFlow("")
    val uiState: StateFlow<String> = _uiState

    init {
        try {
            client = object : WebSocketClient(URI("ws://192.168.1.128:1234"), Draft_6455()) {
                override fun onOpen(handshakedata: ServerHandshake) {
                    println("握手成功")
                }

                override fun onMessage(msg: String) {
                    println("收到消息->${msg}")
                    _uiState.value = msg
                }

                override fun onClose(i: Int, s: String, b: Boolean) {
                    println("链接已关闭")
                }

                override fun onError(e: Exception) {
                    e.printStackTrace()
                    println("发生错误已关闭")
                }
            }
            client?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun send(msg: String) {
        client?.send(msg)
    }
}