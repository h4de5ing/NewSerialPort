package com.android.serialport2.ui

import android_serialport_api.SerialPort
import java.io.File
import kotlin.concurrent.thread

class GoogleSerialPort(path: String, baudRate: Int, val onChange: (ByteArray) -> Unit) :
    SerialPortBase {
    private var serialPort: SerialPort? = null

    init {
        serialPort = SerialPort(File(path), baudRate, 0)
        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val size = serialPort?.inputStream?.read(buffer) ?: 0
                if (size > 0) {
                    val data = ByteArray(size)
                    System.arraycopy(buffer, 0, data, 0, size)
                    println("serial_port ${String(data)}")
                    onChange(data)
                }
            }
        }
    }


    override fun isOpen(): Boolean = serialPort?.isOpen ?: false

    override fun write(data: ByteArray) {
        serialPort?.outputStream?.write(data)
    }

    override fun close() {
        serialPort?.close2()
    }
}