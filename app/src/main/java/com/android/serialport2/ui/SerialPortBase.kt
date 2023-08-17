package com.android.serialport2.ui

interface SerialPortBase {
    fun isOpen():Boolean
    fun write(data: ByteArray)
    fun close()
}