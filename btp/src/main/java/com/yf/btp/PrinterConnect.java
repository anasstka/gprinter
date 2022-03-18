package com.yf.btp;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;


import android.util.Log;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.yf.btp.entity.Printer;

public class PrinterConnect {

    public interface OnControlListener {

        boolean isPicked(BluetoothDevice dev);

        boolean pick(BluetoothDevice dev);

        void connect(BluetoothDevice dev);

        void disconnect(BluetoothDevice dev);
    }

    public final static int BLUETOOTH_DEVICE_CLASS_GPRINTER = 1664;

    public final static int BLUETOOTH_DEVICE_CLASS_MAJOR_GPRINTER = 1536;

    private BluetoothAdapter mBluetoothAdapter;

    private PickedPrinterCallback mPickedPrinterCallback;

    public void setPickedPrinterCallback(PickedPrinterCallback pickedPrinterCallback) {
        this.mPickedPrinterCallback = pickedPrinterCallback;
    }

    private OnControlListener mOnControlListener;

    public OnControlListener getOnControlListener() {
        return mOnControlListener;
    }

    public void setOnControlListener(OnControlListener onControlListener) {
        this.mOnControlListener = onControlListener;
    }

    public PrinterConnect(Context context) {
        BleManager.getInstance().init((Application) context.getApplicationContext());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothDevice getGprinter() {
        if (mBluetoothAdapter != null) {

            BluetoothDevice bluetoothDevice = null;

            for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
                if (device.getBluetoothClass().getMajorDeviceClass() == BLUETOOTH_DEVICE_CLASS_MAJOR_GPRINTER
                        && device.getBluetoothClass().getDeviceClass() == BLUETOOTH_DEVICE_CLASS_GPRINTER)
                    bluetoothDevice = device;

            return bluetoothDevice;
        }

        return null;
    }

    public void onPicked(BluetoothDevice dev) {

        if (mOnControlListener != null)
            if (mOnControlListener.pick(dev)) {
                Log.d("!!!", "Выбрано устройство" + dev.getName());

                if (mPickedPrinterCallback != null)
                    mPickedPrinterCallback.onPicked(new Printer(dev.getName(), dev.getAddress()));

            } else {
                Log.d("!!!", "Устройство недоступно!");
            }

    }


    public void onConnected(BluetoothDevice dev) {
        if (mOnControlListener != null)
            mOnControlListener.connect(dev);
    }

    public void onDisconnected(BluetoothDevice dev) {
        if (mOnControlListener != null)
            mOnControlListener.disconnect(dev);
    }
}

