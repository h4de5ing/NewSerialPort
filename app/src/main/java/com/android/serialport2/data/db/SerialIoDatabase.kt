package com.android.serialport2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SerialIoRecord::class],
    version = 2,
    exportSchema = false,
)
abstract class SerialIoDatabase : RoomDatabase() {
    abstract fun serialIoDao(): SerialIoDao

    companion object {
        @Volatile
        private var INSTANCE: SerialIoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE serial_io_records ADD COLUMN success INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE serial_io_records ADD COLUMN note TEXT")
            }
        }

        fun getInstance(context: Context): SerialIoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SerialIoDatabase::class.java,
                    "serial_io.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
