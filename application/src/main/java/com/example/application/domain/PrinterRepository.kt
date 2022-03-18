package com.example.application.domain

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.util.Size
import com.example.application.R
import com.yf.btp.PrinterService
import com.yf.btp.entity.Printer

interface IPrinterRepository {

    fun checkBluetooth()

    fun onConnect(context: Context): Boolean

    fun onPrint(context: Context, size: Size)

    fun bindService(context: Context)

    fun unbindService(context: Context)

    //    fun registerReceiver(context: Context)
    fun registerReceiver(context: Context, stateOn: () -> Unit, stateOff: () -> Unit)

    fun unregisterReceiver(context: Context)
}

class PrinterRepository : IPrinterRepository, ServiceConnection {

    private var mPrinterService: PrinterService? = null
    private var mPrinter: Printer? = null

    private lateinit var stateOn: () -> Unit
    private lateinit var stateOff: () -> Unit

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state: Int = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d("BroadcastActions", "Bluetooth is off")
                        stateOff()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d("BroadcastActions", "Bluetooth is turning off")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.d("BroadcastActions", "Bluetooth is on")
                        stateOn()
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Log.d("BroadcastActions", "Bluetooth is turning off")
                    }
                }
            }
        }
    }

    override fun checkBluetooth() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.enable()
        }
    }

    override fun onConnect(
        context: Context
    ): Boolean {
        Log.i("!!!", "on connect")
        if (mPrinter == null) {
            if (mPrinterService != null) {
                mPrinterService!!.setPickedPrinterCallback { dev: Printer ->
                    mPrinter = dev
                }
                mPrinterService!!.initPrinterConnect(context)
                val device = mPrinterService!!.printerConnect?.gprinter
                mPrinterService!!.printerConnect?.onConnected(device)
                mPrinterService!!.printerConnect?.onPicked(device)
                return true
            }
            return false
        }
        return true
    }

    override fun onPrint(
        context: Context,
        size: Size
    ) {
        if (mPrinter != null) {
            val icon = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.kit
            )
            mPrinterService!!.printBitmap(icon, Point(0, 0), size, size.width)
        }
    }

    override fun bindService(
        context: Context
    ) {
        if (mPrinterService != null) return

        val intent = Intent(context, PrinterService::class.java)
        context.startService(intent)
        context.bindService(intent, this, BIND_AUTO_CREATE)
    }

    override fun unbindService(
        context: Context
    ) {
        mPrinter = null

        context.unbindService(this)
        context.stopService(Intent(context, PrinterService::class.java))
    }

    override fun registerReceiver(
        context: Context,
        stateOn: () -> Unit,
        stateOff: () -> Unit
    ) {
        this.stateOn = stateOn
        this.stateOff = stateOff

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(mReceiver, filter)
    }

    override fun unregisterReceiver(
        context: Context
    ) {
        context.unregisterReceiver(mReceiver)
    }

    override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
        val binder = service as PrinterService.LocalBinder
        mPrinterService = binder.service
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        mPrinterService = null
    }
}