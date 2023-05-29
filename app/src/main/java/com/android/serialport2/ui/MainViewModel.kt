package com.android.serialport2.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var data2 = MutableLiveData<ByteArray>()
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