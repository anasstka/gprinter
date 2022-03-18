package com.example.application.presentation.main

import android.content.Context
import android.util.Size
import com.example.application.base.BaseViewModel
import com.example.application.base.Reducer
import com.example.application.domain.IPrinterRepository
import com.example.application.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val printerRepository: IPrinterRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<MainScreenState, MainScreenUiEvent>() {

    private val reducer = MainReducer(MainScreenState.initial())

    override val state: StateFlow<MainScreenState>
        get() = reducer.state

    private fun sendEvent(event: MainScreenUiEvent) {
        reducer.sendEvent(event)
    }

    fun setLabelSize(size: Size) {
        settingsRepository.setLabelSize(size)
    }

    fun getLabelSize(): Size = settingsRepository.getLabelSize()

    fun checkBluetooth() {
        printerRepository.checkBluetooth()
//        sendEvent(MainScreenUiEvent.TurnOnBluetooth(status))
    }

    fun onPrint(
        context: Context
    ) {
        printerRepository.onPrint(
            context,
            getLabelSize()
        )
    }

    fun onStart(
        context: Context
    ) {
        printerRepository.registerReceiver(
            context = context,
            stateOn = {
                sendEvent(MainScreenUiEvent.TurnOnBluetooth(true))
            },
            stateOff = {
                sendEvent(MainScreenUiEvent.TurnOnBluetooth(false))
            }
        )
        printerRepository.bindService(
            context
        )
    }

    fun onConnect(
        context: Context
    ) {
        if (state.value.isEnabledBluetooth)
            sendEvent(MainScreenUiEvent.ConnectPrinter(printerRepository.onConnect(context)))
    }

    fun onStop(
        context: Context
    ) {
        printerRepository.unregisterReceiver(context)
        printerRepository.unbindService(
            context
        )
    }

    private class MainReducer(
        initial: MainScreenState
    ) : Reducer<MainScreenState, MainScreenUiEvent>(initial) {

        override fun reduce(oldState: MainScreenState, event: MainScreenUiEvent) {
            when (event) {
                is MainScreenUiEvent.ConnectPrinter -> {
                    setState(oldState.copy(isConnectPrinter = event.status))
                }
                is MainScreenUiEvent.TurnOnBluetooth -> {
                    setState(oldState.copy(isEnabledBluetooth = event.status))
                }
            }
        }
    }
}