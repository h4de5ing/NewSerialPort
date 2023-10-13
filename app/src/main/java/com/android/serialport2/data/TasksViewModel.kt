package com.android.serialport2.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.serialport2.datastore.UserPreferences


class TasksViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow
    suspend fun user(isAuto: Boolean? = null, input: String? = null) {
        userPreferencesRepository.getStore().updateData {
            val default = UserPreferences.getDefaultInstance()
            UserPreferences.newBuilder().setIsAuto(isAuto ?: default.isAuto).build()
        }
    }
}

class TasksViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return TasksViewModel(userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
