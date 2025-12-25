package com.android.serialport2

import android.os.Bundle
import android.text.TextUtils
import android_serialport_api.SerialPortFinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.other.App.Companion.dataSaverPreferences
import com.android.serialport2.other.add
import com.android.serialport2.other.hexToByteArray
import com.android.serialport2.other.toHexString
import com.android.serialport2.ui.Config
import com.android.serialport2.ui.ConfigViewModel
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.SettingsDialog
import com.android.serialport2.ui.TextInputHistorySheet
import com.android.serialport2.ui.WSViewModel
import com.android.serialport2.ui.defaultUri
import com.android.serialport2.ui.theme.NewSerialPortTheme
import com.funny.data_saver.core.LocalDataSaver
import com.funny.data_saver.core.rememberDataSaverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalDataSaver provides dataSaverPreferences) {
                        NavContent()
                    }
                }
            }
        }
    }
}

@Composable
fun NavContent(
    mainView: MainViewModel = viewModel(),
    configView: ConfigViewModel = viewModel(),
    wsView: WSViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val config by configView.uiState.collectAsState(initial = Config())
    val wsConfig by rememberDataSaverState(key = "ws", initialValue = defaultUri)
    val isSync by rememberDataSaverState(key = "sync", initialValue = false)
    val wsServerEnabled by rememberDataSaverState(key = "ws_server_enabled", initialValue = false)
    val wsServerPort by rememberDataSaverState(key = "ws_server_port", initialValue = 8086)
    var showTextInputHistorySheet by remember { mutableStateOf(false) }
    val showingSettingsDialog = remember { mutableStateOf(false) }
    var inputField by remember { mutableStateOf(TextFieldValue(config.input)) }
    fun log(message: String) {
        if (!TextUtils.isEmpty(message)) {
            val current = configView.uiState.value
            configView.update(log = "${current.log}${message}")
        }
    }

    LaunchedEffect(config.isHex) {
        if (config.isHex) {
            val formatted = formatHexText(config.input)
            if (formatted != config.input) configView.update(input = formatted)
        }
    }

    LaunchedEffect(config.input) {
        if (inputField.text != config.input) {
            inputField =
                TextFieldValue(text = config.input, selection = TextRange(config.input.length))
        }
    }

    LaunchedEffect(Unit) {
        val devices = runCatching {
            val finderDevices =
                runCatching { SerialPortFinder().allDevs }.getOrDefault(mutableListOf())
            val fallback = context.resources.getStringArray(R.array.node_index).toList()
            (finderDevices + fallback)
                .distinct()
                .filter { File(it).exists() }
                .sorted()
        }.getOrDefault(emptyList())

        if (devices.isNotEmpty()) {
            configView.update(devices = devices)
            if (config.dev.isBlank() || !devices.contains(config.dev)) {
                configView.update(dev = devices[0])
            }
        }
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                if (config.isAuto && config.isOpen) {
                    val data =
                        if (config.isHex) config.input.hexToByteArray() else config.input.toByteArray()
                    mainView.write(data)
                    configView.update(tx = config.tx + data.size)
                    delay(config.delayTime.toLong())
                }
            }
        }
    }
    LaunchedEffect(mainView.serialData) {
        mainView.serialData.collect {
            val current = configView.uiState.value
            if (isSync && current.isOpen && wsView.isOpen()) {
                val outbound = if (current.isHex) it.toHexString().uppercase() else String(it)
                wsView.send(outbound)
            }
            if (wsServerEnabled && current.isOpen && wsView.isServerRunning()) {
                val outbound = if (current.isHex) it.toHexString().uppercase() else String(it)
                wsView.broadcast(outbound)
            }
            val show = when (current.display) {
                0, 2 -> it.toHexString().uppercase()
                1, 3 -> String(it)
                4 -> "${it.toHexString().uppercase()}\n"
                5 -> "${String(it)}\n"
                else -> ""
            }
            log(show)
            if ((current.display == 2 || current.display == 3) && current.log.length >= 10000) configView.update(
                log = ""
            )
            configView.update(rx = current.rx + it.size)
        }
    }
    LaunchedEffect(config.isOpen, isSync, wsConfig) {
        if (config.isOpen && isSync) {
            wsView.start(wsConfig, onChange = { msg ->
                val current = configView.uiState.value
                configView.update(log = "${current.log}\n${msg}")
                val base = if (current.isHex) msg.hexToByteArray() else msg.toByteArray()
                val data = if (current.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base
                configView.update(tx = current.tx + data.size)
                mainView.write(data)
            })
        } else {
            if (wsView.isOpen()) wsView.close()
        }
    }

    LaunchedEffect(wsServerEnabled, wsServerPort) {
        if (wsServerEnabled) {
            wsView.startServer(wsServerPort) { msg ->
                val current = configView.uiState.value
                configView.update(log = "${current.log}\n${msg}")
                if (current.isOpen) {
                    val base = if (current.isHex) msg.hexToByteArray() else msg.toByteArray()
                    val data = if (current.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base
                    configView.update(tx = current.tx + data.size)
                    mainView.write(data)
                }
            }
        } else {
            if (wsView.isServerRunning()) wsView.stopServer()
        }
    }
    LaunchedEffect(config.log) { scope.launch { scrollState.scrollTo(scrollState.maxValue) } }
    // A bottom sheet to show the text input history to pick from.
    if (showTextInputHistorySheet) {
        val textInputHistory = listOf("历史记录1", "历史记录2", "历史记录3")
        TextInputHistorySheet(
            history = textInputHistory,
            onDismissed = { showTextInputHistorySheet = false },
            onHistoryItemClicked = { item ->
                inputField = TextFieldValue(text = item, selection = TextRange(item.length))
            },
            onHistoryItemDeleted = { item -> },
            onHistoryItemsDeleteAll = { },
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { showingSettingsDialog.value = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "History",
                )
            }
            IconButton(
                onClick = {
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
                    } else {
                        mainView.close()
                    }
                    configView.update(isOpen = mainView.isOpen())
                },
            ) {
                Icon(
                    imageVector = if (config.isOpen) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (config.isOpen) "Close" else "Open",
                )
            }
            Text(
                text = "清空",
                fontSize = 14.sp,
                modifier = Modifier.clickable { configView.update(tx = 0, rx = 0, log = "") })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(1.dp))
                .padding(3.dp)
                .weight(1f)
        ) {
            Text(
                text = config.log, modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Tx:${config.tx}", modifier = Modifier.padding(end = 12.dp))
            Text(text = "Rx:${config.rx}")
        }
        Column {
            Row(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputField,
                    label = { Text("请输入") },
                    onValueChange = { incoming ->
                        if (config.isHex) {
                            val next = formatHexTextFieldValue(incoming)
                            inputField = next
                            if (next.text != config.input) configView.update(input = next.text)
                        } else {
                            inputField = incoming
                            if (incoming.text != config.input) configView.update(input = incoming.text)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                    minLines = 1,
                    leadingIcon = {
                        IconButton(
                            onClick = { showTextInputHistorySheet = true },
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "cd_add_content_icon",
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    })
                if (inputField.text.isNotEmpty() && config.isOpen) {
                    IconButton(
                        colors =
                            IconButtonDefaults.iconButtonColors(containerColor = Color.Blue),
                        onClick = {
                            try {
                                val data =
                                    if (config.isHex) config.input.hexToByteArray() else config.input.toByteArray()
                                configView.update(tx = config.tx + data.size)
                                mainView.write(
                                    if (config.x0D0A) data.add(
                                        byteArrayOf(
                                            0x0D, 0x0A
                                        )
                                    ) else data
                                )
                            } catch (e: Exception) {
                                log("error:${e.message}")
                                e.printStackTrace()
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "send",
                            modifier = Modifier.offset(x = 2.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
        SettingsDialog(showingDialog = showingSettingsDialog, configView = configView, ws = wsView)
    }
}

private fun Char.isHexDigit(): Boolean =
    (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')

private fun formatHexText(raw: String): String {
    val cleaned = buildString {
        for (ch in raw) {
            if (ch.isHexDigit()) append(ch.uppercaseChar())
        }
    }
    if (cleaned.isEmpty()) return ""
    return cleaned.chunked(2).joinToString(" ")
}

private fun formatHexTextFieldValue(incoming: TextFieldValue): TextFieldValue {
    val selectionStart = incoming.selection.start.coerceIn(0, incoming.text.length)
    val hexCountBeforeCursor = incoming.text
        .substring(0, selectionStart)
        .count { it.isHexDigit() }

    val formatted = formatHexText(incoming.text)
    val spacesBeforeCursor = if (hexCountBeforeCursor <= 1) 0 else (hexCountBeforeCursor - 1) / 2
    val newCursor = (hexCountBeforeCursor + spacesBeforeCursor).coerceIn(0, formatted.length)
    return TextFieldValue(text = formatted, selection = TextRange(newCursor))
}