package com.example.application.presentation.main

import androidx.compose.runtime.Immutable
import com.example.application.base.UiEvent
import com.example.application.base.UiState
import com.yf.btp.PrinterService
import com.yf.btp.entity.Printer

@Immutable
sealed class MainScreenUiEvent : UiEvent {
    data class ConnectPrinter(val status: Boolean) : MainScreenUiEvent()
    data class TurnOnBluetooth(val status: Boolean) : MainScreenUiEvent()
}

@Immutable
data class MainScreenState(
    val isConnectPrinter: Boolean,
    val isEnabledBluetooth: Boolean
) : UiState {
    companion object {
        fun initial() = MainScreenState(
            isConnectPrinter = false,
            isEnabledBluetooth = false
        )
    }
}