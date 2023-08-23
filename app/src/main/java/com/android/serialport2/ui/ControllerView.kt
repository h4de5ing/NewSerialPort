package com.android.serialport2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.android.serialport2.EditText
import com.android.serialport2.R
import com.android.serialport2.other.App

@Composable
fun ControllerView() {
    var rx by remember { mutableIntStateOf(0) }
    var tx by remember { mutableIntStateOf(0) }
    var devices =
        stringArrayResource(id = R.array.node_index).toList()// by remember { mutableStateOf(emptyList<String>()) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "串口节点", modifier = Modifier.clickable { })
        MySpinner(items = devices, selectedItem = dev, onItemSelected = { it, _ ->
            dev = it
            App.sp.edit().putString("dev", it).apply()
        })
        Text(text = "波特率")
        MySpinner(items = baudList.toList(), selectedItem = baud, onItemSelected = { it, _ ->
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(top = 5.dp, bottom = 10.dp)
                .fillMaxWidth()
        ) {
            Text(text = "定时(ms)")
            EditText(delayTime, modifier = Modifier.width(50.dp)) {
                if ("^[0-9]{1,4}\$".toRegex().matches(it)) delayTime = it
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Auto")
            Checkbox(checked = isStill, onCheckedChange = { isStill = !isStill })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Hex")
            Checkbox(checked = isHex, onCheckedChange = { isHex = !isHex })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Google")
            Checkbox(checked = isVan, onCheckedChange = { isVan = !isVan })
        }
        Column {
            Text(text = "Tx:${tx}")
            Text(text = "Rx:${rx}")
        }
        Button(onClick = {

        }, shape = RoundedCornerShape(0.dp)) { Text(text = "清除") }
        Button(onClick = {}, shape = RoundedCornerShape(0.dp)) {
            Text(text = if (isOpen) "关闭" else "打开")
        }
    }
}