package com.android.serialport2

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.getInstance().init(this)

    }
}