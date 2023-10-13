package com.android.serialport2

import android.content.Context
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
import androidx.compose.material3.TextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.serialport2.data.TasksViewModel
import com.android.serialport2.data.TasksViewModelFactory
import com.android.serialport2.data.UserPreferencesRepository
import com.android.serialport2.data.UserPreferencesSerializer
import com.android.serialport2.datastore.UserPreferences
import com.android.serialport2.other.App.Companion.dataSaverPreferences
import com.android.serialport2.other.hexToByteArray
import com.android.serialport2.other.toHexString
import com.android.serialport2.ui.Config
import com.android.serialport2.ui.ConfigViewModel
import com.android.serialport2.ui.ControllerView
import com.android.serialport2.ui.MainViewModel
import com.android.serialport2.ui.WSViewModel
import com.android.serialport2.ui.defaultUri
import com.android.serialport2.ui.theme.NewSerialPortTheme
import com.funny.data_saver.core.LocalDataSaver
import com.funny.data_saver.core.rememberDataSaverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val DATA_STORE_FILE_NAME = "user_prefs.pb"

private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(fileName = DATA_STORE_FILE_NAME,
    serializer = UserPreferencesSerializer,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(
            context, USER_PREFERENCES_NAME
        ) { _: SharedPreferencesView, currentData: UserPreferences ->
            currentData
        })
    })

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TasksViewModel

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this, TasksViewModelFactory(UserPreferencesRepository(userPreferencesStore))
        )[TasksViewModel::class.java]

        setContent {
            NewSerialPortTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalDataSaver provides dataSaverPreferences) {
                        NavigationDrawer(calculateWindowSizeClass(this)) { NavContent() }
//                        Test(viewModel)
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
    ModalDrawerSheet(drawerShape = RectangleShape, modifier = Modifier.width(180.dp)) {
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
    var isSync by rememberDataSaverState(key = "sync", initialValue = true)
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
            if (isSync) wsView.send(String(it))
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
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(wsView.uriState) {
        wsView.uriState.collect { uri ->
            var newUri = wsConfig
            if (!TextUtils.isEmpty(uri)) newUri = wsView.uriState.value
            if (wsView.isOpen()) wsView.close()
            wsView.start(newUri, onChange = {
                configView.update(log = "${config.log}\n${it}")
                val data = if (config.isHex) it.hexToByteArray() else it.toByteArray()
                configView.update(tx = config.tx + data.size)
                mainView.write(data)
                scope.launch { scrollState.scrollTo(scrollState.maxValue) }
            })
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
        Column {
//            HexInput(onChange = { configView.update(input = "${config.input}${it}") })
            Row(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InputEditText(
                    config.input, modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                ) {
                    if (config.isHex) {
                        if ("\\A[0-9a-fA-F]+\\z".toRegex().matches(it)) {
                            configView.update(input = it)
                        }
                    } else {
                        configView.update(input = it)
                    }
                }
                Button(
                    onClick = {
                        val data =
                            if (config.isHex) config.input.hexToByteArray() else config.input.toByteArray()
                        configView.update(tx = config.tx + data.size)
                        mainView.write(data)
                    },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.padding(3.dp),
                    enabled = config.isOpen
                ) { Text(text = "发送") }
            }
        }
    }
}

@Composable
fun InputEditText(
    inputValue: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)
) {
    TextField(
        value = inputValue,
        onValueChange = onValueChange,
        modifier = modifier
            .padding(1.dp)
            .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(0.dp)),
        minLines = 1
    )
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