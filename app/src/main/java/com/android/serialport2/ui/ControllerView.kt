package com.android.serialport2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.data.db.SerialIoRepository

@Composable
fun ControllerView(
    mainView: MainViewModel = viewModel(),
    configView: ConfigViewModel = viewModel(),
    ws: WSViewModel = viewModel()
) {
    val context = LocalContext.current
    val ioRepo = remember(context) { SerialIoRepository.getInstance(context) }
    val config by configView.uiState.collectAsState(initial = Config())
    val showingSettings = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
    ) {
        Column {
            Text(text = "Tx:${config.tx}")
            Text(text = "Rx:${config.rx}")
        }
        Button(
            onClick = {
                configView.update(tx = 0, rx = 0, log = "")
                ioRepo.clearAllAsync()
            },
            shape = RoundedCornerShape(0.dp)
        ) { Text(text = "清除") }
        Button(onClick = {
            if (!config.isOpen) {
                try {
                    mainView.setupSerial(
                        path = config.dev,
                        baudRate = config.baud.toInt(),
                        serialType = config.serialType
                    )
                } catch (e: Exception) {
                    configView.update(log = "${config.log}打开异常:${e.message}\n")
                }
            } else mainView.close()
            configView.update(isOpen = mainView.isOpen())
        }, shape = RoundedCornerShape(0.dp)) {
            Text(text = if (config.isOpen) "关闭" else "打开")
        }
        Button(
            onClick = { showingSettings.value = true },
            shape = RoundedCornerShape(0.dp)
        ) { Text(text = "设置") }

        SettingsDialog(showingDialog = showingSettings, configView = configView, ws = ws)
    }
}
