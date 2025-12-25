package com.android.serialport2.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "serial_io_records",
    indices = [
        Index(value = ["timestamp_ms"]),
        Index(value = ["source"]),
        Index(value = ["direction"]),
    ],
)
data class SerialIoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long,

    /**
     * 0 = RX, 1 = TX
     */
    @ColumnInfo(name = "direction")
    val direction: Int,

    /**
     * 0 = SERIAL_RAW, 1 = INPUT_FIELD, 2 = WS_CLIENT, 3 = WS_SERVER
     */
    @ColumnInfo(name = "source")
    val source: Int,

    @ColumnInfo(name = "data")
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerialIoRecord

        if (id != other.id) return false
        if (timestampMs != other.timestampMs) return false
        if (direction != other.direction) return false
        if (source != other.source) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + direction
        result = 31 * result + source
        result = 31 * result + data.contentHashCode()
        return result
    }
}

enum class IoDirection(val code: Int) {
    RX(0),
    TX(1),
}

enum class IoSource(val code: Int) {
    SERIAL_RAW(0),
    INPUT_FIELD(1),
    WS_CLIENT(2),
    WS_SERVER(3),
}
