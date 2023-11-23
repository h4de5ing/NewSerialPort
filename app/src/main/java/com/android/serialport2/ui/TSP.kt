package com.android.serialport2.ui

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TSP(mainView: MainViewModel = viewModel(), configView: ConfigViewModel = viewModel()) {
    val context = LocalContext.current
    val config by configView.uiState.collectAsState(initial = Config())
    LaunchedEffect(mainView.serialData) {
        mainView.serialData.collect {
            configView.update(rx = config.rx + it.size)
        }
    }
    Box {
        Column {
            Text(text = "${config.rx}")
            Row {
                Button(onClick = {
                    Settings.System.putInt(
                        context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE
                    )
                }) { Text(text = "不休眠") }
                Button(onClick = { configView.update(rx = 0) }) { Text(text = "清空") }
                Button(onClick = {
                    if (!config.isOpen) {
                        try {
                            mainView.setupSerial(
                                path = config.dev,
                                baudRate = config.baud.toInt(),
                                isGoogle = config.isGoogle
                            )
                        } catch (e: Exception) {
                            configView.update(log = "${config.log}打开异常:${e.message}\n")
                        }
                    } else mainView.close()
                    configView.update(isOpen = mainView.isOpen())
                }, shape = RoundedCornerShape(0.dp)) {
                    Text(text = if (config.isOpen) "关闭" else "打开")
                }
            }
            Text(text = config.log)
        }
    }
}
