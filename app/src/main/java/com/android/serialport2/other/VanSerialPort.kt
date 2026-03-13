package com.android.serialport2.other

import com.van.uart.LastError
import com.van.uart.UartManager
import kotlin.concurrent.thread

class VanSerialPort(path: String, baudRate: Int, val onChange: (ByteArray) -> Unit) :
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
                val buffer = ByteArray(1024)
                while (uartManager?.isOpen == true) {
                    try {
                        uartManager?.apply {
                            val size = read(buffer, buffer.size, 50, 1)
                            if (size > 0) {
                                val data = ByteArray(size)
                                System.arraycopy(buffer, 0, data, 0, size)
                                onChange(data)
                            }
                        }
                    } catch (_: LastError) {
                        // errno=4 (EINTR) 等可恢复错误，继续重试
                    }
                }
            }
        } catch (e: LastError) {
            onChange(e.toString().toByteArray())
        }
    }

    override fun isOpen(): Boolean {
        return uartManager?.isOpen ?: false
    }

    override fun write(data: ByteArray) {
        try {
            uartManager?.write(data, data.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        try {
            uartManager?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}