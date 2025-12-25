package com.android.serialport2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SerialIoRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class SerialIoDatabase : RoomDatabase() {
    abstract fun serialIoDao(): SerialIoDao

    companion object {
        @Volatile
        private var INSTANCE: SerialIoDatabase? = null

        fun getInstance(context: Context): SerialIoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SerialIoDatabase::class.java,
                    "serial_io.db",
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
