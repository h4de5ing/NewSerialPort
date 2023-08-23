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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.other.App
import com.android.serialport2.other.hexToByteArray
import com.android.serialport2.other.info
import com.android.serialport2.other.toHexString
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.MySpinner
import com.android.serialport2.ui.theme.NewSerialPortTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    HomeContent()
                }
            }
        }
    }
}

@Composable
fun HomeContent(mainView: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf("") }
    var rx by remember { mutableIntStateOf(0) }
    var tx by remember { mutableIntStateOf(0) }
    var devices by remember { mutableStateOf(emptyList<String>()) }
    var isStill by remember { mutableStateOf(false) }
    var isHex by remember { mutableStateOf(false) }
    var isVan by remember { mutableStateOf(false) }
    var delayTime by remember { mutableStateOf("200") }
    var input by remember { mutableStateOf("1B31") }
    var dev by remember { mutableStateOf("") }
    var baud by remember { mutableStateOf("115200") }
    var display by remember { mutableIntStateOf(1) }
    val baudList = stringArrayResource(id = R.array.baud)
    val displayList = stringArrayResource(id = R.array.display)
    var isOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    fun log(message: String) {
        if (!TextUtils.isEmpty(message)) log = "${log}${message}"
    }
    LaunchedEffect(mainView.serialData) {
        mainView.serialData.collect {
            rx += it.size
            val show = when (display) {
                0, 2 -> it.toHexString().uppercase()
                1, 3 -> String(it)
                4 -> "${it.toHexString().uppercase()}\n"
                5 -> "${String(it)}\n"
                else -> ""
            }

            if ((display == 2 || display == 3) && rx >= 10000) log = ""
            log(show)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(isHex) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                if (isStill && isOpen) {
                    val data = if (isHex) input.hexToByteArray() else input.toByteArray()
                    tx += data.size
                    mainView.write(data)
                    delay(delayTime.toLong())
                }
            }
        }
    }
    SideEffect {
        scope.launch {
            withContext(Dispatchers.IO) {
                val newList = SerialPortFinder().allDevs
                newList.addAll(context.resources.getStringArray(R.array.node_index).toList())
                App.sp.getString("dev", "")?.apply { if (!TextUtils.isEmpty(this)) dev = this }
                App.sp.getString("baud", "")?.apply { if (!TextUtils.isEmpty(this)) baud = this }
                display = App.sp.getInt("display", 1)
                devices = newList.distinct().filter { File(it).exists() }.sorted()
                runCatching {
                    if (TextUtils.isEmpty(dev)) dev = devices[0]
                    if (TextUtils.isEmpty(baud)) baud = baudList[display]
                }
            }
        }
    }
    Column {
        Box(
            modifier = Modifier
                .height((LocalConfiguration.current.screenHeightDp * 0.9f).dp)
                .fillMaxWidth()
                .background(Color.LightGray)
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .width((LocalConfiguration.current.screenWidthDp * 0.85f).dp)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(1.dp))
                        .padding(5.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(text = log, modifier = Modifier.fillMaxSize())
                }
                Column(
                    modifier = Modifier
                        .width((LocalConfiguration.current.screenWidthDp * 0.15f).dp)
                        .fillMaxHeight()
                        .padding(5.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "串口节点", modifier = Modifier.clickable { log(info()) })
                    MySpinner(items = devices, selectedItem = dev, onItemSelected = { it, _ ->
                        dev = it
                        App.sp.edit().putString("dev", it).apply()
                    })
                    Text(text = "波特率")
                    MySpinner(
                        items = baudList.toList(),
                        selectedItem = baud,
                        onItemSelected = { it, _ ->
                            baud = it
                            App.sp.edit().putString("baud", it).apply()
                        })
                    Text(text = "显示")
                    MySpinner(items = displayList.toList(),
                        selectedItem = displayList[display],
                        onItemSelected = { _, position ->
                            display = position
                            App.sp.edit().putInt("display", position).apply()
                        })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVan, onCheckedChange = { isVan = !isVan })
                        Text(text = if (isVan) "Van" else "Google")
                    }
                    Column {
                        Text(text = "Tx:${tx}")
                        Text(text = "Rx:${rx}")
                    }
                    Button(onClick = {
                        log = ""
                        tx = 0
                        rx = 0
                    }, shape = RoundedCornerShape(0.dp)) { Text(text = "清除") }
                    Button(onClick = {
                        if (!isOpen) {
                            try {
                                mainView.setupSerial(path = dev, baud.toInt())
                            } catch (e: Exception) {
                                log("串口打开异常原因:${e.message}\n")
                                e.printStackTrace()
                            }
                        } else mainView.close()
                        isOpen = mainView.isOpen()
                    }, shape = RoundedCornerShape(0.dp)) {
                        Text(text = if (isOpen) "关闭" else "打开")
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "定时(ms)")
            EditText(delayTime, modifier = Modifier.width(50.dp)) {
                if ("^[0-9]{1,4}\$".toRegex().matches(it)) delayTime = it
            }
            Text(text = "Auto")
            Checkbox(checked = isStill, onCheckedChange = { isStill = !isStill })
            Text(text = "Hex")
            Checkbox(checked = isHex, onCheckedChange = { isHex = !isHex })
            EditText(input, modifier = Modifier.weight(1f)) {
                if (isHex) {
                    if ("\\A[0-9a-fA-F]+\\z".toRegex().matches(it)) input = it
                } else input = it
            }
            Button(
                onClick = {
                    val data = if (isHex) input.hexToByteArray() else input.toByteArray()
                    tx += data.size
                    mainView.write(data)
                }, shape = RoundedCornerShape(0.dp), enabled = isOpen
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