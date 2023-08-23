package com.android.serialport2.other

interface SerialPortBase {
    fun isOpen():Boolean
    fun write(data: ByteArray)
    fun close()
}