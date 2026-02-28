package com.android.serialport2

import android.os.Bundle
import android.text.TextUtils
import android_serialport_api.SerialPortFinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.android.serialport2.data.db.IoDirection
import com.android.serialport2.data.db.IoSource
import com.android.serialport2.data.db.SerialIoRepository
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
import com.android.serialport2.ui.theme.SourceInputFieldDot
import com.android.serialport2.ui.theme.SourceSerialRawDot
import com.android.serialport2.ui.theme.SourceWsClientDot
import com.android.serialport2.ui.theme.SourceWsServerDot
import com.android.serialport2.ui.theme.StatusFailureDot
import com.android.serialport2.ui.theme.StatusNeutralDot
import com.android.serialport2.ui.theme.StatusSuccessDot
import com.funny.data_saver.core.LocalDataSaver
import com.funny.data_saver.core.rememberDataSaverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
    val context = LocalContext.current
    val ioRepo = remember(context) { SerialIoRepository.getInstance(context) }
    val config by configView.uiState.collectAsState(initial = Config())
    val wsConfig by rememberDataSaverState(key = "ws", initialValue = defaultUri)
    val isSync by rememberDataSaverState(key = "sync", initialValue = false)
    val wsServerEnabled by rememberDataSaverState(key = "ws_server_enabled", initialValue = false)
    val wsServerPort by rememberDataSaverState(key = "ws_server_port", initialValue = 8086)
    var sessionStartMs by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    val records by ioRepo.observeSince(sessionStartMs).collectAsState(initial = emptyList())
    val logListState = rememberLazyListState()
    var showTextInputHistorySheet by remember { mutableStateOf(false) }
    val showingSettingsDialog = remember { mutableStateOf(false) }
    var inputField by remember { mutableStateOf(TextFieldValue(config.input)) }

    fun formatForDisplay(bytes: ByteArray, displayMode: Int): String {
        return when (displayMode) {
            0, 2 -> bytes.toHexString().uppercase()
            1, 3 -> String(bytes)
            else -> ""
        }
    }

    fun log(message: String) {
        if (!TextUtils.isEmpty(message)) {
            // Keep plain text logs for errors only; IO logs are DB-driven.
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
            (finderDevices + fallback).distinct().filter { File(it).exists() }.sorted()
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
                val current = configView.uiState.value
                if (current.isAuto && current.isOpen) {
                    val base =
                        if (current.isHex) current.input.hexToByteArray() else current.input.toByteArray()
                    val writeData = if (current.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base
                    val success = runCatching { mainView.write(writeData) }.isSuccess
                    ioRepo.insertAsync(
                        direction = IoDirection.TX,
                        source = IoSource.INPUT_FIELD,
                        data = writeData,
                        success = success,
                        note = if (success) null else "serial write failed",
                    )
                    if (success) configView.update(tx = current.tx + writeData.size)
                    delay(current.delayTime.toLong())
                } else {
                    delay(50)
                }
            }
        }
    }
    LaunchedEffect(mainView.serialData) {
        mainView.serialData.collect {
            ioRepo.insertAsync(IoDirection.RX, IoSource.SERIAL_RAW, it)
            val current = configView.uiState.value
            if (isSync && current.isOpen && wsView.isOpen()) {
                val outbound = if (current.isHex) it.toHexString().uppercase() else String(it)
                wsView.send(outbound)
            }
            if (wsServerEnabled && current.isOpen && wsView.isServerRunning()) {
                val outbound = if (current.isHex) it.toHexString().uppercase() else String(it)
                wsView.broadcast(outbound)
            }
            configView.update(rx = current.rx + it.size)
        }
    }
    LaunchedEffect(isSync, wsConfig) {
        if (isSync) {
            wsView.start(wsConfig, onChange = { msg ->
                val current = configView.uiState.value
                val base = if (current.isHex) msg.hexToByteArray() else msg.toByteArray()
                val data = if (current.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base
                if (!current.isOpen) {
                    ioRepo.insertAsync(
                        direction = IoDirection.TX,
                        source = IoSource.WS_CLIENT,
                        data = data,
                        success = false,
                        note = "serial closed",
                    )
                    return@start
                }

                val success = runCatching { mainView.write(data) }.isSuccess
                ioRepo.insertAsync(
                    direction = IoDirection.TX,
                    source = IoSource.WS_CLIENT,
                    data = data,
                    success = success,
                    note = if (success) null else "serial write failed",
                )
                if (success) configView.update(tx = current.tx + data.size)
            })
        } else {
            if (wsView.isOpen()) wsView.close()
        }
    }

    LaunchedEffect(wsServerEnabled, wsServerPort) {
        if (wsServerEnabled) {
            wsView.startServer(wsServerPort) { msg ->
                val current = configView.uiState.value
                val base = if (current.isHex) msg.hexToByteArray() else msg.toByteArray()
                val data = if (current.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base

                if (!current.isOpen) {
                    ioRepo.insertAsync(
                        direction = IoDirection.TX,
                        source = IoSource.WS_SERVER,
                        data = data,
                        success = false,
                        note = "serial closed",
                    )
                    return@startServer
                }

                val success = runCatching { mainView.write(data) }.isSuccess
                ioRepo.insertAsync(
                    direction = IoDirection.TX,
                    source = IoSource.WS_SERVER,
                    data = data,
                    success = success,
                    note = if (success) null else "serial write failed",
                )
                if (success) configView.update(tx = current.tx + data.size)
            }
        } else {
            if (wsView.isServerRunning()) wsView.stopServer()
        }
    }
    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) logListState.animateScrollToItem(records.size - 1)
    }
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
                            sessionStartMs = System.currentTimeMillis()
                            configView.update(tx = 0, rx = 0, log = "")
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
                modifier = Modifier.clickable {
                    sessionStartMs = System.currentTimeMillis()
                    configView.update(tx = 0, rx = 0, log = "")
                })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(1.dp))
                .padding(3.dp)
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = logListState,
            ) {
                val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                items(records) { r ->
                    val ts = df.format(Date(r.timestampMs))
                    val isRx = r.direction == IoDirection.RX.code

                    val sourceDotColor = when (r.source) {
                        IoSource.SERIAL_RAW.code -> SourceSerialRawDot
                        IoSource.INPUT_FIELD.code -> SourceInputFieldDot
                        IoSource.WS_CLIENT.code -> SourceWsClientDot
                        IoSource.WS_SERVER.code -> SourceWsServerDot
                        else -> StatusNeutralDot
                    }
                    val successDotColor =
                        if (isRx) StatusNeutralDot else if (r.success) StatusSuccessDot else StatusFailureDot

                    val payload = formatForDisplay(r.data, config.display)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                    ) {
                        Card(
                            modifier = Modifier
                                .align(if (isRx) Alignment.CenterStart else Alignment.CenterEnd),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRx) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier,
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = ts,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(successDotColor, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .background(sourceDotColor, CircleShape),
                                            )
                                        }
                                    }
                                }

                                if (payload.isNotEmpty()) {
                                    Text(text = payload.trimEnd(), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Tx:${config.tx}")
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
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue),
                        onClick = {
                            try {
                                val data =
                                    if (config.isHex) config.input.hexToByteArray() else config.input.toByteArray()
                                val writeData =
                                    if (config.x0D0A) data.add(byteArrayOf(0x0D, 0x0A)) else data
                                val success = runCatching { mainView.write(writeData) }.isSuccess
                                ioRepo.insertAsync(
                                    direction = IoDirection.TX,
                                    source = IoSource.INPUT_FIELD,
                                    data = writeData,
                                    success = success,
                                    note = if (success) null else "serial write failed",
                                )
                                if (success) configView.update(tx = config.tx + writeData.size)
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
    val hexCountBeforeCursor = incoming.text.substring(0, selectionStart).count { it.isHexDigit() }

    val formatted = formatHexText(incoming.text)
    val spacesBeforeCursor = if (hexCountBeforeCursor <= 1) 0 else (hexCountBeforeCursor - 1) / 2
    val newCursor = (hexCountBeforeCursor + spacesBeforeCursor).coerceIn(0, formatted.length)
    return TextFieldValue(text = formatted, selection = TextRange(newCursor))
}