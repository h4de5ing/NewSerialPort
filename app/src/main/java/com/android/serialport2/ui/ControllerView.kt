package com.android.serialport2.ui

import android.text.TextUtils
import android_serialport_api.SerialPortFinder
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.EditText
import com.android.serialport2.R
import com.android.serialport2.other.info
import com.funny.data_saver.core.rememberDataSaverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ControllerView(
    mainView: MainViewModel = viewModel(), configView: ConfigViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by configView.uiState.collectAsState(initial = Config())
    val displayList = stringArrayResource(id = R.array.display)
    val baudList = stringArrayResource(id = R.array.baud)
    val scope = rememberCoroutineScope()
    val showingDialog = remember { mutableStateOf(false) }
    SideEffect {
        scope.launch {
            withContext(Dispatchers.IO) {
                var newList = mutableListOf<String>()
                try {
                    newList = SerialPortFinder().allDevs
                } catch (e: Exception) {
                    //configView.update(log = "${config.log}\n枚举失败，请检查【/proc/tty/drivers】权限问题")
                    e.printStackTrace()
                }
                newList.addAll(context.resources.getStringArray(R.array.node_index).toList())
                val devices = newList.distinct().filter { File(it).exists() }.sorted()
                configView.update(devices = devices)
                runCatching {
                    if (TextUtils.isEmpty(config.dev)) configView.update(dev = devices[0])
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "串口节点",
            modifier = Modifier.clickable { configView.update(log = "${config.log}\n${info()}") })
        MySpinner(items = config.devices, selectedItem = config.dev, onItemSelected = { it, _ ->
            configView.update(dev = it)
        })
        Text(text = "波特率")
        MySpinner(items = baudList.toList(), selectedItem = config.baud, onItemSelected = { it, _ ->
            configView.update(baud = it)
        })
        Text(text = "显示")
        MySpinner(items = displayList.toList(),
            selectedItem = displayList[config.display],
            onItemSelected = { _, position ->
                configView.update(display = position)
            })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(top = 5.dp, bottom = 5.dp)
                .fillMaxWidth()
        ) {
            Text(text = "定时(ms)")
            EditText("${config.delayTime}", modifier = Modifier.width(50.dp)) {
                if ("^[0-9]{1,4}\$".toRegex().matches(it)) configView.update(delayTime = it.toInt())
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Auto")
            Checkbox(checked = config.isAuto, onCheckedChange = { configView.update(isAuto = it) })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Hex")
            Checkbox(checked = config.isHex, onCheckedChange = { configView.update(isHex = it) })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Google")
            Checkbox(checked = config.isGoogle,
                onCheckedChange = { configView.update(isGoogle = it) })
        }
        Column {
            Text(text = "Tx:${config.tx}")
            Text(text = "Rx:${config.rx}")
        }
        Button(
            onClick = { configView.update(tx = 0, rx = 0, log = "") },
            shape = RoundedCornerShape(0.dp)
        ) { Text(text = "清除") }
        Button(onClick = {
            if (!config.isOpen) {
                try {
                    mainView.setupSerial(path = config.dev, config.baud.toInt())
                } catch (e: Exception) {
                    configView.update(log = "${config.log}打开异常:${e.message}\n")
                }
            } else mainView.close()
            configView.update(isOpen = mainView.isOpen())
        }, shape = RoundedCornerShape(0.dp)) {
            Text(text = if (config.isOpen) "关闭" else "打开")
        }
        AlertSettingDialog(showingDialog = showingDialog)
        Button(
            onClick = { showingDialog.value = !showingDialog.value },
            shape = RoundedCornerShape(0.dp)
        ) { Text(text = "设置") }
    }
}

@Composable
private fun AlertSettingDialog(showingDialog: MutableState<Boolean>) {
    var ws by rememberDataSaverState(key = "ws", initialValue = "ws://192.168.1.128:1234")
    var isSync by rememberDataSaverState(key = "sync", initialValue = true)
    if (showingDialog.value) {
        AlertDialog(
            onDismissRequest = { showingDialog.value = false },
            title = { Text(text = "消息转发配置") },
            text = {
                Column(
                    modifier = Modifier
                        .border(1.dp, Color.Black)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(0.dp))
                ) {
                    TextField(modifier = Modifier.fillMaxWidth(), value = ws, onValueChange = {
                        ws = it
                    }, label = { Text("websocket 地址") })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(text = "消息转发")
                        Checkbox(checked = isSync, onCheckedChange = { isSync = it })
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showingDialog.value = false }, modifier = Modifier.padding(16.dp)
                ) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingDialog.value = false
                    }, modifier = Modifier.padding(16.dp)
                ) {
                    Text("确认")
                }
            },
        )
    }
}
