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

    var wsUri by rememberDataSaverState(key = "ws", initialValue = defaultUri)
    var isSync by rememberDataSaverState(key = "sync", initialValue = true)

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
                                    ).width(120.dp),
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
                            title = "Hex",
                            checked = config.isHex
                        ) { configView.update(isHex = it) }
                        ToggleRow(title = "Google", checked = config.isGoogle) {
                            configView.update(
                                isGoogle = it
                            )
                        }
                        ToggleRow(
                            title = "0D0A",
                            checked = config.x0D0A
                        ) { configView.update(x0D0A = it) }
                    }

                    SettingsSection(title = "消息转发") {
                        SpinnerEdit(
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = false,
                            hint = "websocket 地址",
                            value = wsUri,
                            items = emptyList(),
                        ) { _, v ->
                            wsUri = v
                        }
                        ToggleRow(title = "消息转发", checked = isSync) { isSync = it }
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