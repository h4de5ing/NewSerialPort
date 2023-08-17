package com.android.serialport2.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private var data2 = MutableLiveData<ByteArray>()
    private val _uartData = MutableStateFlow(ByteArray(0))
    val uartData = _uartData.asStateFlow()
    fun updateUartData(data: ByteArray) {
        _uartData.value = data
    }
    fun update(data: ByteArray) {
        this.data2.value = data
    }

    val counterLiveData: LiveData<Long>
        get() = counter

    private val counter = MutableLiveData<Long>()

    fun increaseCounter(value: Long) {
        counter.value = counter.value?.plus(value)
    }
}