package com.android.serialport2

import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.ui.theme.NewSerialPortTheme
import com.android.serialport2.ui.viewmodel.SerialViewModel
import kotlinx.coroutines.flow.onEach

class TestMain2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Test()
                }
            }
        }
    }
}

@Composable
fun Test() {
    var log by remember { mutableStateOf("") }
    fun log(message: String) {
        log = "${log}${message}\n"
    }

    val viewModel = viewModel<SerialViewModel>()
    val serialData = viewModel.serialData.collectAsState()
    LaunchedEffect(true) {
        viewModel._serialData.onEach {
            log("有数据来了:${String(it)}")
        }
    }
    var isStill by remember { mutableStateOf(false) }
    var isHex by remember { mutableStateOf(false) }
    val inputValue = remember { mutableStateOf("1B31") }
    val scrollState = rememberScrollState()
    var tx by remember { mutableStateOf(0) }
    var rx by remember { mutableStateOf(0) }
    var isOpen by remember { mutableStateOf(false) }

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
                    Text(text = log, fontSize = 14.sp)
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
                    Text(text = "状态")
                    Column {
                        Text(text = "Tx:${tx}")
                        Text(text = "Rx:${rx}")
                    }
                    Button(onClick = {
                        log = ""
                        tx = 0
                        rx = 0
                    }, shape = RoundedCornerShape(0.dp)) {
                        Text(text = "清除")
                    }

                    Button(onClick = {
                        val result = kotlin.runCatching {
                            viewModel.setupSerial("/dev/ttyUSB0", 115200)
                        }
                        isOpen = result.isSuccess
                        log(if (result.isSuccess) "打开成功" else "打开失败")
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