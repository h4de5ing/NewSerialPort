package com.android.serialport2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.serialport2.other.GoogleSerialPort
import com.android.serialport2.other.JSerialCommPort
import com.android.serialport2.other.SerialPortBase
import com.android.serialport2.other.VanSerialPort
import com.android.serialport2.other.toHexString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var serial: SerialPortBase? = null
    private val _serialData = MutableStateFlow(ByteArray(0))
    val serialData = _serialData.asStateFlow()
    fun setupSerial(path: String, baudRate: Int, serialType: Int) {
        val callback: (ByteArray) -> Unit = {
            viewModelScope.launch { _serialData.value = it }
        }
        serial = when (serialType) {
            1 -> GoogleSerialPort(path, baudRate, callback)
            2 -> JSerialCommPort(path, baudRate, callback)
            else -> VanSerialPort(path, baudRate, callback)
        }
    }

    fun isOpen(): Boolean = serial?.isOpen() ?: false

    fun write(data: ByteArray) {
        println("${serial?.javaClass}_(${String(data)}):" + data.toHexString())
        serial?.write(data)
    }

    fun close() {
        serial?.close()
    }

    override fun onCleared() = Unit
}