package com.android.serialport2

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.getInstance().init(this)
    }
}