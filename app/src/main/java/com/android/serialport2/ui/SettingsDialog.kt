package com.android.serialport2.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.BuildConfig
import com.android.serialport2.R
import com.funny.data_saver.core.rememberDataSaverState
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun SettingsDialog(
    showingDialog: MutableState<Boolean>,
    configView: ConfigViewModel = viewModel(),
    ws: WSViewModel = viewModel(),
) {
    if (!showingDialog.value) return

    val config by configView.uiState.collectAsState(initial = Config())
    val baudList = stringArrayResource(id = R.array.baud)
    val displayList = stringArrayResource(id = R.array.display)
    val serialTypeList = stringArrayResource(id = R.array.serial_type)

    var wsUri by rememberDataSaverState(key = "ws", initialValue = defaultUri)
    var wsClientEnabled by rememberDataSaverState(key = "sync", initialValue = false)
    var wsServerEnabled by rememberDataSaverState(key = "ws_server_enabled", initialValue = false)
    var wsServerPort by rememberDataSaverState(key = "ws_server_port", initialValue = 8086)

    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    fun dismiss() {
        ws.updateUri(wsUri)
        showingDialog.value = false
    }

    Dialog(onDismissRequest = { dismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "App version: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SettingsSection(title = "串口设置") {
                        SpinnerEdit(
                            modifier = Modifier.fillMaxWidth(),
                            items = config.devices,
                            hint = "串口节点",
                            value = config.dev,
                        ) { _, it ->
                            configView.update(dev = it)
                        }
                        SpinnerEdit(
                            modifier = Modifier.fillMaxWidth(),
                            items = baudList.toList(),
                            hint = "波特率",
                            value = config.baud,
                            readOnly = true
                        ) { _, it ->
                            configView.update(baud = it)
                        }
                    }

                    SettingsSection(title = "输入输出") {
                        SpinnerEdit(
                            modifier = Modifier.fillMaxWidth(),
                            items = displayList.toList(),
                            hint = "显示方式",
                            value = displayList.getOrElse(config.display) {
                                displayList.firstOrNull() ?: ""
                            },
                            readOnly = true
                        ) { index, _ ->
                            if (index >= 0) configView.update(display = index)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "定时(ms)")
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    .width(120.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                OutlinedTextField(value = "${config.delayTime}", onValueChange = {
                                    if ("^[0-9]{1,5}$".toRegex().matches(it)) {
                                        configView.update(delayTime = it.toInt())
                                    }
                                })
                            }
                        }

                        ToggleRow(title = "Auto", checked = config.isAuto) {
                            configView.update(
                                isAuto = it
                            )
                        }
                        ToggleRow(
                            title = "Hex", checked = config.isHex
                        ) { configView.update(isHex = it) }
                        SpinnerEdit(
                            modifier = Modifier.fillMaxWidth(),
                            items = serialTypeList.toList(),
                            hint = "串口库",
                            value = serialTypeList.getOrElse(config.serialType) {
                                serialTypeList.firstOrNull() ?: ""
                            },
                            readOnly = true
                        ) { index, _ ->
                            if (index >= 0) configView.update(serialType = index)
                        }
                        ToggleRow(
                            title = "0D0A", checked = config.x0D0A
                        ) { configView.update(x0D0A = it) }
                    }

                    SettingsSection(title = "WebSocket") {
                        ToggleRow(title = "WS客户端", checked = wsClientEnabled) {
                            wsClientEnabled = it
                            if (!it) ws.close()
                        }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = wsUri,
                            enabled = wsClientEnabled,
                            onValueChange = { wsUri = it },
                            label = { Text(text = "WS 地址") },
                            singleLine = true,
                        )

                        ToggleRow(title = "WS服务端", checked = wsServerEnabled) {
                            wsServerEnabled = it
                        }
                        if (wsServerEnabled) {
                            val ip = remember { getLocalIpAddress() }
                            Text(
                                text = "本机IP: ${ip.ifBlank { "未知" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = wsServerPort.toString(),
                                onValueChange = { raw ->
                                    val trimmed = raw.trim()
                                    if (trimmed.isEmpty()) return@OutlinedTextField
                                    if (!"^[0-9]{1,5}$".toRegex()
                                            .matches(trimmed)
                                    ) return@OutlinedTextField
                                    val p = trimmed.toIntOrNull() ?: return@OutlinedTextField
                                    if (p in 1..65535) wsServerPort = p
                                },
                                label = { Text(text = "端口") },
                                singleLine = true,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = { dismiss() }) {
                        Text(text = "关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun getLocalIpAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        interfaces.asSequence().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }.filterIsInstance<Inet4Address>()
            .map { it.hostAddress ?: "" }.firstOrNull { it.isNotBlank() && it != "127.0.0.1" }
            .orEmpty()
    } catch (_: Exception) {
        ""
    }
}