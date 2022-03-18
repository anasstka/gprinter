package com.example.application.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.yf.btp.PrinterService

//class ServiceConnectionImpl(
//    var printerService: PrinterService?
//) : ServiceConnection {
//    override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
//        val binder = service as PrinterService.LocalBinder
//        printerService = binder.service
//    }
//
//    override fun onServiceDisconnected(className: ComponentName?) {
//        printerService = null
//    }
//}