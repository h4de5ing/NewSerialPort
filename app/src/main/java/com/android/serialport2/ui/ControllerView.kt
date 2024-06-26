package com.android.serialport2.ui

import android.text.TextUtils
import android_serialport_api.SerialPortFinder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import java.io.File

@Composable
fun ControllerView(
    mainView: MainViewModel = viewModel(),
    configView: ConfigViewModel = viewModel(),
    ws: WSViewModel = viewModel()
) {
    var wsConfig by rememberDataSaverState(key = "ws", initialValue = "ws://192.168.1.128:1234")
    val context = LocalContext.current
    val config by configView.uiState.collectAsState(initial = Config())
    val displayList = stringArrayResource(id = R.array.display)
    val baudList = stringArrayResource(id = R.array.baud)
    val scope = rememberCoroutineScope()
    val showingDialog = remember { mutableStateOf(false) }
    val expandSetting = remember { mutableStateOf(false) }
    val sync by ws.sync.collectAsState()
    LaunchedEffect(Unit) {
        var newList = mutableListOf<String>()
        try {
            newList = SerialPortFinder().allDevs
        } catch (e: Exception) {
            //configView.update(log = "${config.log}\n枚举失败，请检查【/proc/tty/drivers】权限问题")
            //e.printStackTrace()
        }
        newList.addAll(context.resources.getStringArray(R.array.node_index).toList())
        val devices = newList.distinct().filter { File(it).exists() }.sorted()
        configView.update(devices = devices)
        if (TextUtils.isEmpty(config.dev)) configView.update(dev = devices[0])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SpinnerEdit(items = config.devices, hint = "串口节点", value = config.dev) { _, it ->
            configView.update(dev = it)
        }
        SpinnerEdit(
            items = baudList.toList(),
            hint = "波特率",
            value = config.baud,
            readOnly = true
        ) { _, it ->
            configView.update(baud = it)
        }
        SpinnerEdit(
            items = displayList.toList(),
            hint = "显示方式",
            value = displayList[config.display],
            readOnly = true
        ) { index, _ ->
            configView.update(display = index)
        }
        Column {
            Text(text = "Tx:${config.tx}")
            Text(text = "Rx:${config.rx}")
        }
        ExpandController(expandSetting, "设置") { expandSetting.value = it }
        if (expandSetting.value) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "定时(ms)")
                EditText("${config.delayTime}", modifier = Modifier.width(50.dp)) {
                    if ("^[0-9]{1,4}\$".toRegex()
                            .matches(it)
                    ) configView.update(delayTime = it.toInt())
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Auto")
                Checkbox(
                    checked = config.isAuto,
                    onCheckedChange = { configView.update(isAuto = it) })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Hex")
                Checkbox(
                    checked = config.isHex,
                    onCheckedChange = { configView.update(isHex = it) })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Google", modifier = Modifier.clickable {
                    configView.update(log = "${config.log}\n${info()}")
                })
                Checkbox(checked = config.isGoogle,
                    onCheckedChange = { configView.update(isGoogle = it) })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "0D0A")
                Checkbox(
                    checked = config.x0D0A,
                    onCheckedChange = { configView.update(x0D0A = it) })
            }
        }
//        Icon(
//            painter = painterResource(id = if (sync) R.drawable.baseline_sync_24 else R.drawable.baseline_sync_disabled_24),
//            contentDescription = "Data Swap"
//        )
        Button(
            onClick = { configView.update(tx = 0, rx = 0, log = "") },
            shape = RoundedCornerShape(0.dp)
        ) { Text(text = "清除") }
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
        AlertSettingDialog(showingDialog = showingDialog) {
            wsConfig = it
            ws.updateUri(it)
        }
//        Button(
//            onClick = { showingDialog.value = !showingDialog.value },
//            shape = RoundedCornerShape(0.dp)
//        ) { Text(text = "设置") }
    }
}

@Composable
private fun AlertSettingDialog(showingDialog: MutableState<Boolean>, onChange: ((String) -> Unit)) {
    var ws by rememberDataSaverState(key = "ws", initialValue = defaultUri)
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
                        onChange(ws)
                        showingDialog.value = false
                    }, modifier = Modifier.padding(16.dp)
                ) {
                    Text("确认")
                }
            },
        )
    }
}

@Composable
fun ExpandController(expand: MutableState<Boolean>, title: String, onChange: ((Boolean) -> Unit)) {
    Column {
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    expand.value = !expand.value
                    onChange(expand.value)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title)
            Icon(
                imageVector = if (expand.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = ""
            )
        }
    }
}