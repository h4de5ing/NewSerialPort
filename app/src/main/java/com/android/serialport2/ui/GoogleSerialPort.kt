package com.android.serialport2.ui

import com.van.uart.LastError
import com.van.uart.UartManager
import kotlin.concurrent.thread

class GoogleSerialPort(path: String, baudRate: Int, val onChange: (ByteArray) -> Unit) :
    SerialPortBase {
    private var uartManager: UartManager? = null

    init {
        try {
            uartManager = UartManager()
            uartManager?.open(
                path.split("/dev/")[1],
                UartManager.getBaudRate(baudRate)
            )
            thread {
                while (true) {
                    val buffer = ByteArray(1024)
                    uartManager?.apply {
                        val size = read(buffer, buffer.size, 50, 1)
                        if (size > 0) {
                            val data = ByteArray(size)
                            System.arraycopy(buffer, 0, data, 0, size)
                            println("uartManager ${String(data)}")
                            onChange(data)
                        }
                    }
                }
            }
        } catch (e: LastError) {
            e.printStackTrace()
        }
    }

    override fun isOpen(): Boolean {
        return uartManager?.isOpen ?: false
    }

    override fun write(data: ByteArray) {
        uartManager?.write(data, data.size)
    }

    override fun close() {
        uartManager?.close()
    }
}