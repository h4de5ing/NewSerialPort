package com.android.serialport2.other

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

class App : Application() {
    companion object{
        lateinit var sp: SharedPreferences
    }
    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
    }
}