package com.android.serialport2.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.EditText
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

	val expandSerial = remember { mutableStateOf(true) }
	val expandIo = remember { mutableStateOf(true) }
	val expandWs = remember { mutableStateOf(false) }

	AlertDialog(
		onDismissRequest = { showingDialog.value = false },
		title = { Text(text = "设置") },
		text = {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.verticalScroll(rememberScrollState())
					.padding(top = 4.dp)
			) {
				ExpandController(expandSerial, "串口设置") { expandSerial.value = it }
				if (expandSerial.value) {
					Column(
						modifier = Modifier
							.border(1.dp, Color.Black, RoundedCornerShape(0.dp))
							.padding(8.dp)
					) {
						SpinnerEdit(
							items = config.devices,
							hint = "串口节点",
							value = config.dev,
						) { _, it ->
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
					}
				}

				ExpandController(expandIo, "输入输出") { expandIo.value = it }
				if (expandIo.value) {
					Column(
						modifier = Modifier
							.border(1.dp, Color.Black, RoundedCornerShape(0.dp))
							.padding(8.dp)
					) {
						SpinnerEdit(
							items = displayList.toList(),
							hint = "显示方式",
							value = displayList.getOrElse(config.display) { displayList.firstOrNull() ?: "" },
							readOnly = true
						) { index, _ ->
							if (index >= 0) configView.update(display = index)
						}

						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
							modifier = Modifier
								.padding(top = 8.dp)
								.fillMaxWidth()
						) {
							Text(text = "定时(ms)")
							EditText("${config.delayTime}", modifier = Modifier.width(80.dp)) {
								if ("^[0-9]{1,5}$".toRegex().matches(it)) {
									configView.update(delayTime = it.toInt())
								}
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
								onCheckedChange = { configView.update(isAuto = it) }
							)
						}

						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
							modifier = Modifier.fillMaxWidth()
						) {
							Text(text = "Hex")
							Checkbox(
								checked = config.isHex,
								onCheckedChange = { configView.update(isHex = it) }
							)
						}

						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
							modifier = Modifier.fillMaxWidth()
						) {
							Text(
								text = "Google",
								modifier = Modifier.padding(end = 8.dp)
							)
							Checkbox(
								checked = config.isGoogle,
								onCheckedChange = { configView.update(isGoogle = it) }
							)
						}

						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
							modifier = Modifier.fillMaxWidth()
						) {
							Text(text = "0D0A")
							Checkbox(
								checked = config.x0D0A,
								onCheckedChange = { configView.update(x0D0A = it) }
							)
						}
					}
				}

				ExpandController(expandWs, "消息转发") { expandWs.value = it }
				if (expandWs.value) {
					Column(
						modifier = Modifier
							.border(1.dp, Color.Black, RoundedCornerShape(0.dp))
							.padding(8.dp)
					) {
						TextField(
							modifier = Modifier.fillMaxWidth(),
							value = wsUri,
							onValueChange = {
								wsUri = it
							},
							label = { Text("websocket 地址") }
						)
						Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Text(text = "消息转发")
							Checkbox(checked = isSync, onCheckedChange = { isSync = it })
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					ws.updateUri(wsUri)
					showingDialog.value = false
				},
				modifier = Modifier.padding(8.dp)
			) {
				Text("完成")
			}
		},
	)
}