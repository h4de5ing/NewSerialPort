package com.android.serialport2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HexInput(onChange: ((String) -> Unit)) {
    FlowRow(modifier = Modifier
        .padding(top = 10.dp)
        .fillMaxWidth()
        .background(Color.LightGray)) {
        (0..15).forEach {
            val text = it.toString(16).uppercase()
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(30.dp)
                    .wrapContentHeight()
                    .background(Color.Transparent)
                    .clickable { onChange(text) },
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            )
        }
    }
}