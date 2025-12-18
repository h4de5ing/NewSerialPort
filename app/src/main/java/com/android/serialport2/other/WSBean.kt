package com.android.serialport2.other

data class WSBean(var time: Long, var msg: String) {
    override fun toString(): String = "${time.date()}: $msg"
}