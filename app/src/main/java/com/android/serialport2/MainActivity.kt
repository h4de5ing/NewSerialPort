package com.android.serialport2

import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.android.serialport2.ui.MySpinner
import com.android.serialport2.ui.theme.NewSerialPortTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    private val devList = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope.launch {
            withContext(Dispatchers.IO) {
                val mSerialPortFinder = SerialPortFinder()
                val list: MutableList<String> = ArrayList(mSerialPortFinder.allDevs)
                for (s in resources.getStringArray(R.array.node_index)) if (File(s).exists() && !list.contains(
                        s
                    )
                ) list.add(s)
                devList.addAll(list)
            }
        }
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    var log by remember { mutableStateOf("log:") }
                    var isStill by remember { mutableStateOf(false) }
                    var isHex by remember { mutableStateOf(false) }
                    val delayTime = remember { mutableStateOf("200") }
                    val inputValue = remember { mutableStateOf("1B31") }
                    var dev by remember { mutableStateOf("") }
                    var baud by remember { mutableStateOf("115200") }
                    var display by remember { mutableStateOf("Hex") }
                    var tx by remember { mutableStateOf(0) }
                    var rx by remember { mutableStateOf(0) }
                    val baudList = stringArrayResource(id = R.array.baud)
                    val displayList = stringArrayResource(id = R.array.display)
                    fun log(message: String) {
                        log = "${log}${message}\n"
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
                                ) { Text(text = log, fontSize = 14.sp) }
                                Column(
                                    modifier = Modifier
                                        .width(
                                            (LocalConfiguration.current.screenWidthDp * 0.15f).dp
                                        )
                                        .fillMaxHeight()
                                        .padding(5.dp)
                                ) {
                                    Text(text = "串口节点")
                                    MySpinner(
                                        items = devList,
                                        selectedItem = dev,
                                        onItemSelected = {
                                            dev = it
                                            log("选择了串口:${it}")
                                        })
                                    Text(text = "波特率")
                                    MySpinner(
                                        items = baudList.toList(),
                                        selectedItem = baud,
                                        onItemSelected = {
                                            baud = it
                                            log("选择了波特率:${it}")
                                        })
                                    Text(text = "显示")
                                    MySpinner(
                                        items = displayList.toList(),
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
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "清除")
                                    }
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "打开")
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
//                                .height((LocalConfiguration.current.screenHeightDp * 0.1f).dp)
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "定时")
                            EditText(delayTime)
                            Text(text = "Auto")
                            Checkbox(checked = isStill, onCheckedChange = { isStill = !isStill })
                            EditText(inputValue)
                            Text(text = "Hex")
                            Checkbox(checked = isHex, onCheckedChange = { isHex = !isHex })
                            Button(onClick = {}) { Text(text = "发送") }
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
                width = 0.5.dp,
                color = Color.Black,
                shape = RoundedCornerShape(1.dp)
            )
            .padding(3.dp),
    )
}