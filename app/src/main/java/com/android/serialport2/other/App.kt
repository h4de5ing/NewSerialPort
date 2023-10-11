package com.android.serialport2.other

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.funny.data_saver.core.DataSaverInterface
import com.funny.data_saver.core.DataSaverPreferences

class App : Application() {
    companion object {
        lateinit var sp: SharedPreferences
        lateinit var dataSaverPreferences: DataSaverInterface
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        dataSaverPreferences = DataSaverPreferences(sp, senseExternalDataChange = true)
    }
}