package com.android.serialport2.data.db

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SerialIoRepository private constructor(context: Context) {
    private val dao = SerialIoDatabase.getInstance(context).serialIoDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun insertAsync(direction: IoDirection, source: IoSource, data: ByteArray) {
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
