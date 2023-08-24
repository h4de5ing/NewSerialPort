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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.other.App
import com.android.serialport2.other.hexToByteArray
import com.android.serialport2.other.toHexString
import com.android.serialport2.ui.Config
import com.android.serialport2.ui.ConfigViewModel
import com.android.serialport2.ui.ControllerView
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.theme.NewSerialPortTheme
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
                    NavigationDrawer(calculateWindowSizeClass(this)) { NavContent() }
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
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { DrawerContent() }) { content() }
    } else {
        PermanentNavigationDrawer(drawerContent = { DrawerContent() }) { content() }
    }
}

@Composable
fun DrawerContent() {
    ModalDrawerSheet(drawerShape = RectangleShape, modifier = Modifier.width(180.dp)) {
        ControllerView()
    }
}

@Composable
fun NavContent(mainView: MainViewModel = viewModel(), configView: ConfigViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val config by configView.uiState.collectAsState(initial = Config())
    fun log(message: String) {
        if (!TextUtils.isEmpty(message)) {
            configView.update(log = "${config.log}${message}")
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
            val show = when (config.display) {
                0, 2 -> it.toHexString().uppercase()
                1, 3 -> String(it)
                4 -> "${it.toHexString().uppercase()}\n"
                5 -> "${String(it)}\n"
                else -> ""
            }
            log(show)
            if ((config.display == 2 || config.display == 3) && config.log.length >= 10000)
                configView.update(log = "")
            configView.update(rx = config.rx + it.size)
            scrollState.scrollTo(scrollState.maxValue)
        }
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
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .padding(3.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditText(
                config.input, modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
            ) {
                if (config.isHex) {
                    if ("\\A[0-9a-fA-F]+\\z".toRegex().matches(it)) {
                        configView.update(input = it)
                        App.sp.edit().putString("input", it).apply()
                    }
                } else {
                    configView.update(input = it)
                    App.sp.edit().putString("input", it).apply()
                }
            }
            Button(
                onClick = {
                    val data =
                        if (config.isHex) config.input.hexToByteArray() else config.input.toByteArray()
                    configView.update(tx = config.tx + data.size)
                    mainView.write(data)
                }, shape = RoundedCornerShape(0.dp), enabled = config.isOpen
            ) { Text(text = "发送") }
        }
    }
}

@Composable
fun EditText(inputValue: String, modifier: Modifier = Modifier, onValueChange: ((String) -> Unit)) {
    BasicTextField(
        value = inputValue,
        onValueChange = onValueChange,
        modifier = modifier
            .border(
                width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(1.dp)
            )
            .padding(3.dp),
    )
}