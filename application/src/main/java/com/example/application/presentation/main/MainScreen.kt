package com.example.application.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.application.presentation.main.components.CustomDialog
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showCustomDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    viewModel.onStart(context)
    viewModel.checkBluetooth()

    LaunchedEffect(key1 = state) {
        if (state.isEnabledBluetooth) {
            delay(1000)
            viewModel.onConnect(context)
        }
    }

    DisposableEffect(key1 = lifecycle) {
//        scope.launch(Dispatchers.Main) {
//            viewModel.onStart(context)
//            viewModel.checkBluetooth()
//            if (state.isEnabledBluetooth) {
//                delay(1000)
//                viewModel.onConnect(context)
//            }
//        }
        onDispose {
            viewModel.onStop(context)
        }
    }

    val text =
        if (state.isEnabledBluetooth) if (state.isConnectPrinter) "Принтер подключен" else "Принтер не подключен" else "Bluetooth выключен"

    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text)

            Spacer(modifier = Modifier.height(20.dp))

            if (!state.isConnectPrinter) {
                Button(onClick = {
                    if (state.isEnabledBluetooth)
                        viewModel.onConnect(context)
                }) {
                    Text(text = "ПОДКЛЮЧИТЬ")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Button(onClick = {
                showCustomDialog = !showCustomDialog
            }) {
                Text(text = "НАСТРОИТЬ")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                if (state.isEnabledBluetooth)
                    viewModel.onPrint(context)
            }) {
                Text(text = "ПЕЧАТЬ")
            }
        }
    }

    if (showCustomDialog) {
        CustomDialog(
            labelSize = viewModel.getLabelSize(),
            onDismiss = {
                showCustomDialog = !showCustomDialog
            },
            onNegativeClick = {
                showCustomDialog = !showCustomDialog
            },
            onPositiveClick = { size ->
                showCustomDialog = !showCustomDialog
                viewModel.setLabelSize(size)
            }
        )
    }
}