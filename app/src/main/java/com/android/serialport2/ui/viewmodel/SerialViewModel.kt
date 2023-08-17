package com.android.serialport2.ui.viewmodel

import android_serialport_api.SerialPort
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.concurrent.thread

class SerialViewModel : ViewModel() {

    val _serialData = MutableStateFlow(ByteArray(0))
    val serialData = _serialData.asStateFlow()

    private var serialPort: SerialPort? = null

    fun setupSerial(path: String, baudRate: Int) {
        serialPort = SerialPort(File(path), baudRate, 0, 8, 1, 0)
        thread {
            while (serialPort?.isOpen == true) {
                serialPort?.inputStream?.readBytes()?.apply {
                    _serialData.value = this
                }
            }
        }
    }

    override fun onCleared() {
        serialPort?.close2()
    }
}