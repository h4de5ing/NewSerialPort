package com.android.serialport2

import android.os.Bundle
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.other.App.Companion.dataSaverPreferences
import com.android.serialport2.other.add
import com.android.serialport2.other.hexToByteArray
import com.android.serialport2.other.toHexString
import com.android.serialport2.ui.Config
import com.android.serialport2.ui.ConfigViewModel
import com.android.serialport2.ui.ControllerView
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.TextInputHistorySheet
import com.android.serialport2.ui.WSViewModel
import com.android.serialport2.ui.defaultUri
import com.android.serialport2.ui.theme.NewSerialPortTheme
import com.funny.data_saver.core.LocalDataSaver
import com.funny.data_saver.core.rememberDataSaverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalDataSaver provides dataSaverPreferences) {
                        NavigationDrawer(calculateWindowSizeClass(this)) { NavContent() }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationDrawer(
    windowSizeClass: WindowSizeClass, content: @Composable () -> Unit
) {
    val windowWidthSizeClass = windowSizeClass.widthSizeClass
    if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = { DrawerContent() }) {
            Column {
                Text(text = "向右滑打开控制菜单")
                content()
            }
        }
    } else {
        PermanentNavigationDrawer(drawerContent = { DrawerContent() }) { content() }
    }
}

@Composable
fun DrawerContent() {
    ModalDrawerSheet(drawerShape = RectangleShape, modifier = Modifier.width(200.dp)) {
        ControllerView()
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
    val config by configView.uiState.collectAsState(initial = Config())
    val wsConfig by rememberDataSaverState(key = "ws", initialValue = defaultUri)
    val isSync by rememberDataSaverState(key = "sync", initialValue = true)
    var showTextInputHistorySheet by remember { mutableStateOf(false) }
    var inputField by remember { mutableStateOf(TextFieldValue(config.input)) }
    fun log(message: String) {
        if (!TextUtils.isEmpty(message)) {
            configView.update(log = "${config.log}${message}")
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
            if (isSync && config.isOpen && wsView.isOpen()) {
                val outbound = if (config.isHex) it.toHexString().uppercase() else String(it)
                wsView.send(outbound)
            }
            val show = when (config.display) {
                0, 2 -> it.toHexString().uppercase()
                1, 3 -> String(it)
                4 -> "${it.toHexString().uppercase()}\n"
                5 -> "${String(it)}\n"
                else -> ""
            }
            log(show)
            if ((config.display == 2 || config.display == 3) && config.log.length >= 10000) configView.update(
                log = ""
            )
            configView.update(rx = config.rx + it.size)
        }
    }
    LaunchedEffect(config.isOpen, isSync, wsConfig) {
        if (config.isOpen && isSync) {
            wsView.start(wsConfig, onChange = { msg ->
                configView.update(log = "${config.log}\n${msg}")
                val base = if (config.isHex) msg.hexToByteArray() else msg.toByteArray()
                val data = if (config.x0D0A) base.add(byteArrayOf(0x0D, 0x0A)) else base
                configView.update(tx = config.tx + data.size)
                mainView.write(data)
            })
        } else {
            if (wsView.isOpen()) wsView.close()
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
            },
            onHistoryItemDeleted = { item -> },
            onHistoryItemsDeleteAll = { },
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
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
    }
}

@Composable
fun EditText(inputValue: String, modifier: Modifier = Modifier, onValueChange: ((String) -> Unit)) {
    BasicTextField(
        value = inputValue, onValueChange = onValueChange, modifier = modifier
            .border(
                width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(1.dp)
            )
            .padding(3.dp), minLines = 1
    )
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