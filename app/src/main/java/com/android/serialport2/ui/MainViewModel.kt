package com.android.serialport2.ui

import android_serialport_api.SerialPort
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.concurrent.thread

class MainViewModel : ViewModel() {
    private var serialPort: SerialPort? = null
    private val _serialData = MutableStateFlow(ByteArray(0))
    val serialData = _serialData.asStateFlow()
    fun updateData(data: ByteArray){
        _serialData.value=data
    }
    fun setupSerial(path: String, baudRate: Int) {
        serialPort = SerialPort(File(path), baudRate, 0)
        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val size = serialPort?.inputStream?.read(buffer) ?: 0
                if (size > 0) {
                    val data = ByteArray(size)
                    System.arraycopy(buffer, 0, data, 0, size)
                    println("serial_port ${String(data)}")
                    viewModelScope.launch { _serialData.value = data }
                }
            }
        }
    }

    fun isOpen(): Boolean = serialPort?.isOpen ?: false

    fun write(data: ByteArray) {
        serialPort?.outputStream?.write(data)
    }

    fun close() {
        serialPort?.close2()
    }

    override fun onCleared() {
        serialPort?.close2()
    }
}