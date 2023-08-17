package com.android.serialport2

import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.MySpinner
import com.android.serialport2.ui.theme.NewSerialPortTheme
import com.van.uart.LastError
import com.van.uart.UartManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    private var serialPort: SerialPort? = null
    private var uartManager: UartManager? = null
    private var readBytes: ByteArray? = null
    private var uiState by mutableStateOf(String)
    private var mainView = MainViewModel()
    //TODO 通过ViewModel共享数据变化
    /**
     * 将源数组追加到目标数组
     *
     * @param byte1 Sou1原数组1
     * @param byte2 Sou2原数组2
     * @param size   长度
     * @return  返回一个新的数组，包括了原数组1和原数组2
     */
    private fun arrayAppend(byte1: ByteArray?, byte2: ByteArray?, size: Int): ByteArray? {
        return if (byte1 == null && byte2 == null) {
            null
        } else if (byte1 == null) {
            val byte3 = ByteArray(size)
            System.arraycopy(byte2, 0, byte3, 0, size)
            byte3
        } else if (byte2 == null) {
            val byte3 = ByteArray(byte1.size)
            System.arraycopy(byte1, 0, byte3, 0, byte1.size)
            byte3
        } else {
            val byte3 = ByteArray(byte1.size + size)
            System.arraycopy(byte1, 0, byte3, 0, byte1.size)
            System.arraycopy(byte2, 0, byte3, byte1.size, size)
            byte3
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            try {
                while (true) {
                    try {
                        val buffer = ByteArray(2048)
                        uartManager?.apply {
                            if (isOpen) {
                                val length = read(buffer, buffer.size, 50, 1)
                                val data = ByteArray(length)
                                System.arraycopy(buffer, 0, data, 0, length)
                                println("vr:" + String(data))
                                mainView.update(data)
                                mainView.increaseCounter(length * 1L)
                            }
                        }
                        serialPort?.apply {
                            if (isOpen) {
                                val size = if (inputStream.available() == 0) 0
                                else inputStream.read(buffer)
                                println("gr:${size}")
                                mainView.updateUartData("gr:${size} ${System.currentTimeMillis()}".toByteArray())
                                if (size > 0) {
                                    readBytes = arrayAppend(readBytes, buffer, size)
                                } else {
                                    readBytes?.apply {
                                        var sum = 0x00
                                        forEach { sum += it }
                                        if (sum != 0) {
                                            println("gr:${String(this)}")
                                            mainView.update(this)
                                            mainView.increaseCounter(size * 1L)
                                        }
                                    }
                                    readBytes = null
                                }
                                //TODO 更新数据
                            }
                        }
                    } catch (_: Exception) {
                    }
                    SystemClock.sleep(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        //TODO 默认值保存到sp里面，第一次加载上次保存
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val scrollState = rememberScrollState()
                    val viewModel: MainViewModel = viewModel()
//                    val data2 = model.data2.observeAsState()
//                    val rx = model.counterLiveData.observeAsState(0)
                    var devices by remember { mutableStateOf(emptyList<String>()) }
                    var log by remember { mutableStateOf("") }
                    var isStill by remember { mutableStateOf(false) }
                    var isHex by remember { mutableStateOf(false) }
                    var isVan by remember { mutableStateOf(false) }
                    val delayTime = remember { mutableStateOf("200") }
                    val inputValue = remember { mutableStateOf("1B31") }
                    var dev by remember { mutableStateOf("") }
                    var baud by remember { mutableStateOf("115200") }
                    var display by remember { mutableStateOf("Hex") }
                    var tx by remember { mutableStateOf(0) }
                    var rx by remember { mutableStateOf(0) }
                    val baudList = stringArrayResource(id = R.array.baud)
                    val displayList = stringArrayResource(id = R.array.display)
                    var isOpen by remember { mutableStateOf(false) }
                    fun log(message: String) {
                        log = "${log}${message}\n"
                    }
                    val uartData by viewModel.uartData.collectAsState()

//                    Text(text = uartData.toString(Charsets.UTF_8))
                    SideEffect {
                        println("hello side Effect")
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val newList = SerialPortFinder().allDevs
                                newList.addAll(
                                    resources.getStringArray(R.array.node_index).toList()
                                )
                                devices = newList.distinct().filter { File(it).exists() }.sorted()
                                kotlin.runCatching { if (TextUtils.isEmpty(dev)) dev = devices[0] }
                            }
                        }
                    }
                    Column {
                        Box(
                            modifier = Modifier
                                .height(
                                    (LocalConfiguration.current.screenHeightDp * 0.9f).dp
                                )
                                .fillMaxWidth()
                                .background(Color.LightGray)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(
                                            (LocalConfiguration.current.screenWidthDp * 0.85f).dp
                                        )
                                        .fillMaxHeight()
                                        .border(
                                            width = 1.dp,
                                            color = Color.Black,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                        .padding(5.dp)
                                ) {
                                    Text(text = uartData.toString(Charsets.UTF_8), fontSize = 14.sp)
                                }
                                Column(
                                    modifier = Modifier
                                        .width(
                                            (LocalConfiguration.current.screenWidthDp * 0.15f).dp
                                        )
                                        .fillMaxHeight()
                                        .padding(5.dp)
                                        .verticalScroll(scrollState)
                                ) {
                                    Text(text = "串口节点")
                                    MySpinner(items = devices,
                                        selectedItem = dev,
                                        onItemSelected = {
                                            dev = it
                                            log("选择了串口:${it}")
                                        })
                                    Text(text = "波特率")
                                    MySpinner(items = baudList.toList(),
                                        selectedItem = baud,
                                        onItemSelected = {
                                            baud = it
                                            log("选择了波特率:${it}")
                                        })
                                    Text(text = "显示")
                                    MySpinner(items = displayList.toList(),
                                        selectedItem = display,
                                        onItemSelected = {
                                            display = it
                                            log("选择了数据显示方式:${it}")
                                        })
                                    Text(text = "状态")
                                    Column {
                                        Text(text = "Tx:${tx}")
                                        Text(text = "Rx:${rx}")
                                    }
                                    Button(onClick = {
                                        log = ""
                                        tx = 0
                                        rx = 0
                                        mainView.increaseCounter(0L)
                                    }, shape = RoundedCornerShape(0.dp)) {
                                        Text(text = "清除")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = if (isVan) "Van" else "Google")
                                        Checkbox(checked = isVan,
                                            onCheckedChange = { isVan = !isVan })
                                    }
                                    Button(onClick = {
                                        if (isVan) {
                                            if (isOpen) {
                                                uartManager?.close()
                                                isOpen = false
                                            } else {
                                                try {
                                                    uartManager = UartManager()
                                                    uartManager?.open(
                                                        dev.split("/dev/")[1],
                                                        UartManager.getBaudRate(baud.toInt())
                                                    )
                                                    isOpen = true
                                                } catch (e: LastError) {
                                                    log("打开异常: $e")
                                                }
                                            }
                                        } else {
                                            if (isOpen) {
                                                serialPort?.close2()
                                                isOpen = false
                                            } else {
                                                try {
                                                    serialPort = SerialPort(
                                                        File(dev), baud.toInt(), 0
                                                    )
                                                    isOpen = true
                                                } catch (e: Exception) {
                                                    log("打开异常: $e")
                                                }
                                            }
                                        }
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
                            EditText(delayTime)
                            Text(text = "Auto")
                            Checkbox(checked = isStill, onCheckedChange = { isStill = !isStill })
                            Text(text = "Hex")
                            Checkbox(checked = isHex, onCheckedChange = { isHex = !isHex })
                            Column(Modifier.weight(1f)) {
                                BasicTextField(
                                    value = inputValue.value,
                                    onValueChange = { inputValue.value = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 0.5.dp,
                                            color = Color.Black,
                                            shape = RoundedCornerShape(1.dp)
                                        )
                                        .padding(3.dp),
                                )
                            }
                            Button(
                                onClick = {}, shape = RoundedCornerShape(0.dp),
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                            ) { Text(text = "发送") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditText(inputValue: MutableState<String>) {
    BasicTextField(
        value = inputValue.value,
        onValueChange = { inputValue.value = it },
        modifier = Modifier
            .border(
                width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(1.dp)
            )
            .padding(3.dp),
    )
}