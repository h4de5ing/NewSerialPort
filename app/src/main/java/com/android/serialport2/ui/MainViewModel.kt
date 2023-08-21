package com.android.serialport2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.van.uart.LastError
import com.van.uart.UartManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class MainViewModel : ViewModel() {
    private var uartManager: UartManager? = null
    private val _serialData = MutableStateFlow(ByteArray(0))
    val serialData = _serialData.asStateFlow()
    fun updateData(data: ByteArray) {
        _serialData.value = data
    }

    fun setupSerial(path: String, baudRate: Int) {
        uartManager = UartManager()
        try {
            uartManager?.open(
                path.split("/dev/")[1], UartManager.getBaudRate(baudRate)
            )
        } catch (e: LastError) {
            viewModelScope.launch { _serialData.value = "$e".toByteArray() }
        }
        thread {
            while (true) {
                val buffer = ByteArray(1024)
                uartManager?.apply {
                    val size = read(buffer, buffer.size, 50, 1)
                    if (size > 0) {
                        val data = ByteArray(size)
                        System.arraycopy(buffer, 0, data, 0, size)
                        println("uartManager ${String(data)}")
                        viewModelScope.launch { _serialData.value = data }
                    }
                }
            }
        }
    }

    fun isOpen(): Boolean = uartManager?.isOpen ?: false

    fun write(data: ByteArray) {
        uartManager?.write(data, data.size)
    }

    fun close() {
        uartManager?.close()
    }

    override fun onCleared() = Unit
}