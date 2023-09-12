package com.android.serialport2.data

import kotlinx.serialization.Serializable


@Serializable
data class UserPreferencesSerializerBean(
    val isAuto: Boolean,
    val isHex: Boolean,
    val isGoogle: Boolean,
    val isOpen: Boolean,
    val delayTime: Int,
    val display: Int,
    val tx: Long,
    val rx: Long,
    val input: String,
)
