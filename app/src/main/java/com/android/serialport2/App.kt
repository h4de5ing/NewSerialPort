package com.android.serialport2

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

class App : Application() {
    companion object{
        lateinit var sp: SharedPreferences
    }
    override fun onCreate() {
        super.onCreate()
        CrashHandler.getInstance().init(this)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
    }
}