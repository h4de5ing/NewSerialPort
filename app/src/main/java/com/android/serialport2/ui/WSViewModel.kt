package com.android.serialport2.ui

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

const val defaultUri = "ws://192.168.1.128:1234"

class WSViewModel : ViewModel() {
    private var client: WebSocketClient? = null
    private val _uriState = MutableStateFlow("")
    val uriState: StateFlow<String> = _uriState
    private val _sync = MutableStateFlow(false)
    val sync: StateFlow<Boolean> = _sync

    fun updateUri(uri: String) {
        _uriState.value = uri
    }

    fun start(uri: String, onChange: ((String) -> Unit)) {
        try {
            client = object : WebSocketClient(URI(uri), Draft_6455()) {
                override fun onOpen(handshakedata: ServerHandshake) {
                    _sync.value = true
                }

                override fun onMessage(msg: String) {
                    _sync.value = true
                    if (!TextUtils.isEmpty(msg)) onChange(msg)
                }

                override fun onClose(i: Int, s: String, b: Boolean) {
                    _sync.value = false
                }

                override fun onError(e: Exception) {
                    _sync.value = false//尝试重连
                    e.printStackTrace()
                }
            }
            client?.connect()
        } catch (e: Exception) {
            _sync.value = false
            e.printStackTrace()
        }
    }

    fun isOpen(): Boolean = client != null && client?.isOpen == true
    fun close() {
        client?.apply { if (this.isOpen) this.close() }
    }

    fun send(msg: String) {
        client?.send(msg)
    }
}