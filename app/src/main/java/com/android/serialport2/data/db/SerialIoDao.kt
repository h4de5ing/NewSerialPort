package com.android.serialport2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SerialIoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: SerialIoRecord): Long

    @Query("SELECT * FROM serial_io_records ORDER BY id DESC LIMIT :limit")
    suspend fun latest(limit: Int = 200): List<SerialIoRecord>

    @Query(
        "SELECT * FROM serial_io_records " +
            "WHERE timestamp_ms >= :sinceMs " +
            "ORDER BY id ASC " +
            "LIMIT :limit"
    )
    fun observeSince(sinceMs: Long, limit: Int = 2000): Flow<List<SerialIoRecord>>

    @Query("DELETE FROM serial_io_records")
    suspend fun clearAll()
}
