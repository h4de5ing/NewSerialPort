package com.android.serialport2.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class TasksViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow

    fun showCompletedTasks(isAuto: Boolean? = null, input: String? = null) {
        viewModelScope.launch {
            userPreferencesRepository.updateShowCompleted(isAuto, input)
        }
    }

    fun show(
        onComplete: ((UserPreferencesRepository) -> Unit)
    ) {
        viewModelScope.launch { onComplete(userPreferencesRepository) }
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
