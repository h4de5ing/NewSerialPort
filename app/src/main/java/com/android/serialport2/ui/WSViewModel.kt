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

    fun updateUri(uri: String) {
        _uriState.value = uri
    }

    fun start(uri: String, onChangeTips: ((String) -> Unit) = {}, onChange: ((String) -> Unit)) {
        try {
            client = object : WebSocketClient(URI(uri), Draft_6455()) {
                override fun onOpen(handshakedata: ServerHandshake) {
                    onChangeTips("握手成功")
                }

                override fun onMessage(msg: String) {
                    println("收到消息->${msg}")
                    if (!TextUtils.isEmpty(msg)) onChange(msg)
                }

                override fun onClose(i: Int, s: String, b: Boolean) {
                    onChangeTips("链接已关闭")
                }

                override fun onError(e: Exception) {
                    onChangeTips("发生错误已关闭")//尝试重连
                    e.printStackTrace()
                }
            }
            client?.connect()
        } catch (e: Exception) {
            onChangeTips("发生异常:${e.message}")
            e.printStackTrace()
        }
    }

    fun isOpen(): Boolean = client != null && client?.isOpen == true
    fun close() {
        client?.apply {
            if (this.isOpen) this.close()
        }
    }

    fun send(msg: String) {
        client?.send(msg)
    }
}