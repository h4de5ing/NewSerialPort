package com.android.serialport2.ui.viewmodel

import android_serialport_api.SerialPort
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SerialViewModel : ViewModel() {
    private var serialPort: SerialPort? = null

    private val _serialData = MutableStateFlow(ByteArray(0))
    val serialData = _serialData.asStateFlow()

    fun setupSerial(path: String, baudRate: Int) {

    }

    fun isOpen(): Boolean = serialPort?.isOpen ?: false

    fun write(data: ByteArray) {
        serialPort?.outputStream?.write(data)
    }

    override fun onCleared() {
        serialPort?.close2()
    }
}