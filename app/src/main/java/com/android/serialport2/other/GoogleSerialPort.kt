package com.android.serialport2.other

import android_serialport_api.SerialPort
import java.io.File
import kotlin.concurrent.thread

class GoogleSerialPort(path: String, baudRate: Int, val onChange: (ByteArray) -> Unit) :
    SerialPortBase {
    private var serialPort: SerialPort? = null

    init {
        val file = File(path)
        if (file.exists()) {
            serialPort = SerialPort(File(path), baudRate, 0)
            thread {
                while (true) {
                    val buffer = ByteArray(1024)
                    try {
                        val size = serialPort?.inputStream?.read(buffer) ?: 0
                        if (size > 0) {
                            val data = ByteArray(size)
                            System.arraycopy(buffer, 0, data, 0, size)
                            onChange(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else onChange("$path 不存在".toByteArray())
    }


    override fun isOpen(): Boolean = serialPort?.isOpen ?: false

    override fun write(data: ByteArray) {
        try {
            serialPort?.outputStream?.write(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        try {
            serialPort?.close2()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}