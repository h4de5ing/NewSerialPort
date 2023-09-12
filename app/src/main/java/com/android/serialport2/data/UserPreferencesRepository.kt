package com.android.serialport2.data

import android.util.Log
import androidx.datastore.core.DataStore
import com.android.serialport2.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

class UserPreferencesRepository(private val userPreferencesStore: DataStore<UserPreferences>) {
    val userPreferencesFlow: Flow<UserPreferences> = userPreferencesStore.data.catch { exception ->
        if (exception is IOException) {
            Log.e("", "Error reading sort order preferences.", exception)
            emit(UserPreferences.getDefaultInstance())
        } else {
            throw exception
        }
    }

    suspend fun updateShowCompleted(isAuto: Boolean? = null, input: String? = null) {
        userPreferencesStore.updateData { currentPreferences ->
            currentPreferences.toBuilder().setIsAuto(isAuto ?: currentPreferences.isAuto)
                .setInput(input ?: currentPreferences.input).build()
        }
    }
}