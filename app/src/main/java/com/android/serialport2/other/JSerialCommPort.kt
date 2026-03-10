package com.android.serialport2.other

import com.fazecast.jSerialComm.SerialPort
import kotlin.concurrent.thread

class JSerialCommPort(path: String, baudRate: Int, val onChange: (ByteArray) -> Unit) :
    SerialPortBase {
    private var serialPort: SerialPort? = null

    init {
        try {
            serialPort = SerialPort.getCommPort(path).apply {
                setBaudRate(baudRate)
                setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0)
            }
            if (serialPort?.openPort() == true) {
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
            } else onChange("$path open failed".toByteArray())
        } catch (e: Exception) {
            onChange("${e.message}".toByteArray())
        }
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
            serialPort?.closePort()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
