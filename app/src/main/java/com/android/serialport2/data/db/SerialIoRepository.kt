package com.android.serialport2.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SerialIoRepository private constructor(context: Context) {
    private val dao = SerialIoDatabase.getInstance(context).serialIoDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun clearAllAsync() {
        scope.launch { dao.clearAll() }
    }

    fun observeSince(sinceMs: Long, limit: Int = 2000): Flow<List<SerialIoRecord>> {
        return dao.observeSince(sinceMs = sinceMs, limit = limit)
    }

    fun insertAsync(direction: IoDirection, source: IoSource, data: ByteArray) {
        insertAsync(direction = direction, source = source, data = data, success = true, note = null)
    }

    fun insertAsync(
        direction: IoDirection,
        source: IoSource,
        data: ByteArray,
        success: Boolean,
        note: String?,
    ) {
        if (data.isEmpty()) return
        val copy = data.copyOf()
        val now = System.currentTimeMillis()
        scope.launch {
            dao.insert(
                SerialIoRecord(
                    timestampMs = now,
                    direction = direction.code,
                    source = source.code,
                    data = copy,
                    success = success,
                    note = note,
                ),
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SerialIoRepository? = null

        fun getInstance(context: Context): SerialIoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SerialIoRepository(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }
}
