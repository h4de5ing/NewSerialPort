package com.android.serialport2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.android.serialport2.data.TasksViewModel
import com.android.serialport2.data.UserPreferencesSerializerBean
import com.android.serialport2.datastore.UserPreferences
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun Test(viewModel: TasksViewModel) {
    val userPreferences =
        viewModel.userPreferencesFlow.collectAsState(initial = UserPreferences.getDefaultInstance()).value
    var text by remember { mutableStateOf("$userPreferences") }
    val scope = rememberCoroutineScope()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "isAuto")
            Checkbox(checked = userPreferences.isAuto, onCheckedChange = {
                scope.launch { viewModel.user(isAuto = it) }
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditText(inputValue = userPreferences.input) {
                scope.launch { viewModel.user(input = it) }
            }
            Button(onClick = {
                val user = UserPreferencesSerializerBean(
                    isAuto = userPreferences.isAuto,
                    isHex = userPreferences.isHex,
                    isGoogle = userPreferences.isGoogle,
                    isOpen = userPreferences.isOpen,
                    delayTime = userPreferences.delayTime,
                    display = userPreferences.display,
                    tx = userPreferences.tx,
                    rx = userPreferences.rx,
                    input = userPreferences.input,
                )

                text = Json.encodeToString(user)
            }) {
                Text(text = "序列化与反序列化")
            }
        }
        Text(text = text)
    }
}
