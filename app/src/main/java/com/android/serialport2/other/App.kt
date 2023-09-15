package com.android.serialport2.other

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
    }
}