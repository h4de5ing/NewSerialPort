package com.android.serialport2.ui

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI

const val defaultUri = "ws://10.18.16.247:8086"

class WSViewModel : ViewModel() {
    private var client: WebSocketClient? = null
    private var connectedUri: String? = null
    private val _uriState = MutableStateFlow("")
    val uriState: StateFlow<String> = _uriState
    private val _sync = MutableStateFlow(false)
    val sync: StateFlow<Boolean> = _sync

    private var server: WebSocketServer? = null
    private var serverPort: Int? = null
    private val _serverRunning = MutableStateFlow(false)
    val serverRunning: StateFlow<Boolean> = _serverRunning

    fun updateUri(uri: String) {
        _uriState.value = uri
    }

    fun start(uri: String, onChange: ((String) -> Unit)) {
        val trimmed = uri.trim()
        _uriState.value = trimmed
        if (trimmed.isEmpty()) {
            _sync.value = false
            return
        }
        if (client?.isOpen == true && connectedUri == trimmed) return

        close()

        try {
            connectedUri = trimmed
            client = object : WebSocketClient(URI(trimmed), Draft_6455()) {
                override fun onOpen(handshakedata: ServerHandshake) {
                    _sync.value = true
                }

                override fun onMessage(msg: String) {
                    _sync.value = true
                    if (!TextUtils.isEmpty(msg)) onChange(msg)
                }

                override fun onClose(code: Int, reason: String, remote: Boolean) {
                    _sync.value = false
                }

                override fun onError(e: Exception) {
                    _sync.value = false
                    e.printStackTrace()
                }
            }
            client?.connect()
        } catch (e: Exception) {
            _sync.value = false
            connectedUri = null
            client = null
        }
    }

    fun isOpen(): Boolean = client != null && client?.isOpen == true
    fun close() {
        try {
            client?.apply { if (this.isOpen) this.close() }
        } catch (_: Exception) {
        } finally {
            client = null
            connectedUri = null
            _sync.value = false
        }
    }

    fun startServer(port: Int, onMessage: (String) -> Unit) {
        if (port !in 1..65535) return
        if (server != null && _serverRunning.value && serverPort == port) return

        stopServer()

        try {
            serverPort = port
            server = object : WebSocketServer(InetSocketAddress(port)) {
                override fun onStart() {
                    _serverRunning.value = true
                }

                override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
                    // no-op
                }

                override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
                    // no-op
                }

                override fun onMessage(conn: WebSocket, message: String) {
                    if (!TextUtils.isEmpty(message)) onMessage(message)
                }

                override fun onError(conn: WebSocket?, ex: Exception) {
                    ex.printStackTrace()
                }
            }
            server?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            server = null
            serverPort = null
            _serverRunning.value = false
        }
    }

    fun stopServer() {
        try {
            val s = server
            if (s != null) {
                runCatching { s.stop(500) }
            }
        } catch (_: Exception) {
        } finally {
            server = null
            serverPort = null
            _serverRunning.value = false
        }
    }

    fun isServerRunning(): Boolean = server != null && _serverRunning.value

    fun broadcast(msg: String) {
        if (msg.isEmpty()) return
        val s = server ?: return
        if (!_serverRunning.value) return
        runCatching { s.broadcast(msg) }
    }

    fun send(msg: String) {
        if (client?.isOpen == true) {
            try {
                client?.send(msg)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        close()
        stopServer()
    }
}