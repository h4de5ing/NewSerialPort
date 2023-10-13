package com.android.serialport2.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ConfigViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(Config())
    val uiState: StateFlow<Config> = _uiState

    fun update(
        devices: List<String> = _uiState.value.devices,
        delayTime: Int = _uiState.value.delayTime,
        isAuto: Boolean = _uiState.value.isAuto,
        isHex: Boolean = _uiState.value.isHex,
        isGoogle: Boolean = _uiState.value.isGoogle,
        display: Int = _uiState.value.display,
        dev: String = _uiState.value.dev,
        baud: String = _uiState.value.baud,
        isOpen: Boolean = _uiState.value.isOpen,
        tx: Long = _uiState.value.tx,
        rx: Long = _uiState.value.rx,
        log: String = _uiState.value.log,
        input: String = _uiState.value.input
    ) {
        _uiState.value = _uiState.value.copy(
            devices = devices,
            isAuto = isAuto,
            isHex = isHex,
            isGoogle = isGoogle,
            delayTime = delayTime,
            display = display,
            dev = dev,
            baud = baud,
            isOpen = isOpen,
            tx = tx,
            rx = rx,
            log = log,
            input = input,
        )
    }
}

data class Config(
    val devices: List<String> = emptyList(),
    val delayTime: Int = 200,
    var isAuto: Boolean = false,
    val isHex: Boolean = false,
    val isGoogle: Boolean = true,
    val display: Int = 1,
    val tx: Long = 0,
    val rx: Long = 0,
    val input: String = "",
    val dev: String = "",
    val baud: String = "115200",
    val log: String = "",
    val isOpen: Boolean = false
)