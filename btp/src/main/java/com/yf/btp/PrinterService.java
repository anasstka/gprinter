package com.yf.btp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;
import com.yf.btp.utils.ArrayUtils;
import com.yf.btp.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class PrinterService extends Service implements IPrinterFeatures {

    public PrinterConnect printerConnect;

    public final static int PORT_TYPE_BLUETOOTH = PortParameters.BLUETOOTH;

    public final static int PORT_TYPE_SERIAL = PortParameters.SERIAL;

    public static final String TAG = PrinterService.class.getCanonicalName();

    public static final String ACTION_CONNECT_STATUS = "action.connect.status";

    private GpService mGpService;

    private PrinterServiceConnection mConn;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private static int BT_DEV_ID_GEN = 0;

    private ArrayMap<String, Integer> macIdMap = new ArrayMap<>();

    private List<PrinterConnectStatCallback> mPrinterConnectStatCallbacks = new ArrayList<>();

    private PickedPrinterCallback mPickedPrinterCallback;

    private int mPickPrinter = -1; // but the printer selected before


    public PickedPrinterCallback getPickedPrinterCallback() {
        return mPickedPrinterCallback;
    }

    public void setPickedPrinterCallback(PickedPrinterCallback pickedPrinterCallback) {
        this.mPickedPrinterCallback = pickedPrinterCallback;
    }

    public void addPrinterConnectStatCallback(PrinterConnectStatCallback callback) {
        mPrinterConnectStatCallbacks.add(callback);
    }

    public void removePrinterConnectStatCallback(PrinterConnectStatCallback callback) {
        mPrinterConnectStatCallbacks.remove(callback);
    }


    public class LocalBinder extends Binder {

        public PrinterService getService() {

            // Return this instance of LocalService so clients can call public methods
            return PrinterService.this;
        }
    }


    private void bindService() {

        mConn = new PrinterServiceConnection();

        Intent intent = new Intent(this, GpPrintService.class);

        bindService(intent, mConn, Context.BIND_AUTO_CREATE); // bindService

    }

    private void unbindService() {

        unbindService(mConn);
    }


    class PrinterServiceConnection implements ServiceConnection {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("^_* ServiceConnection", "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.i("^_* ServiceConnection", "onServiceConnected() called");

            mGpService = GpService.Stub.asInterface(service);

            Log.i("^_* ServiceConnection", mGpService.toString());

        }
    }


    private BroadcastReceiver PrinterStatusBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_CONNECT_STATUS.equals(intent.getAction())) {

                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);

                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);

                String mac = printerIdToMac(id);

                Log.d(TAG, "^_* connect status " + id + ":" + type);

                if (type == GpDevice.STATE_CONNECTING) {

                    for (PrinterConnectStatCallback callback : mPrinterConnectStatCallbacks)
                        callback.onConnecting(mac);

                } else if (type == GpDevice.STATE_NONE) {

                    macIdMap.remove(mac);

                    for (PrinterConnectStatCallback callback : mPrinterConnectStatCallbacks)
                        callback.onFailure(mac);

                } else if (type == GpDevice.STATE_VALID_PRINTER) {

                    for (PrinterConnectStatCallback callback : mPrinterConnectStatCallbacks)
                        callback.onConnected(mac);

                } else if (type == GpDevice.STATE_INVALID_PRINTER) {

                    macIdMap.remove(mac);

                    for (PrinterConnectStatCallback callback : mPrinterConnectStatCallbacks)
                        callback.onFailure(mac);
                }

            }
        }
    };


    private void registerBroadcast() {

        IntentFilter filter = new IntentFilter();

        filter.addAction(ACTION_CONNECT_STATUS);

        registerReceiver(PrinterStatusBroadcastReceiver, filter);
    }

    private void unRegisterBroadcast() {

        unregisterReceiver(PrinterStatusBroadcastReceiver);

    }

    public PrinterService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerBroadcast();

        bindService();
    }


    private String printerIdToMac(int printerId) {

        List<Integer> values = new ArrayList<>(macIdMap.values());

        for (int i = 0; i < values.size(); i++) {

            if (values.get(i) == printerId) {

                return macIdMap.keyAt(i);
            }
        }

        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isInitPrint = false;

        mPickedPrinterCallback = null;

        unRegisterBroadcast();

        unbindService();
    }

    public void initPrinterConnect(Context context) {

        printerConnect = new PrinterConnect(context);

        printerConnect.setPickedPrinterCallback(mPickedPrinterCallback);

        printerConnect.setOnControlListener(new PrinterConnect.OnControlListener() {

            @Override
            public boolean isPicked(BluetoothDevice dev) {

                Integer id = macIdMap.get(dev.getAddress());

                if (id != null && id == mPickPrinter) return true;

                return false;
            }

            @Override
            public boolean pick(BluetoothDevice dev) {

                Integer id = macIdMap.get(dev.getAddress());

                if (id == null) return false;

                mPickPrinter = id;

                return true;
            }

            @Override
            public void connect(BluetoothDevice dev) {

                int devId = BT_DEV_ID_GEN++;

                macIdMap.put(dev.getAddress(), devId);

                if (0 == openPort(devId, dev.getAddress())) {
                    Log.d("printerConnect", "Printer Connect");
                }
            }

            @Override
            public void disconnect(BluetoothDevice dev) {

                Integer devId = macIdMap.remove(dev.getAddress());

                if (devId != null)
                    closePort(devId);

            }
        });
    }


    /**
     * isValid
     * <p>
     * Return true to currently select an available device，return false, otherwise
     */
    @Override
    public boolean isValid() {

        return mPickPrinter != -1;
    }


    /**
     * getSelectedDevName
     * <p>
     * Returns the name of the currently selected printer device， Returns null if no device is currently selected
     */
    @Override
    public String getSelectedDevName() {

        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(printerIdToMac(mPickPrinter));

        if (remoteDevice != null)
            return remoteDevice.getName();

        return null;
    }

    @Override
    public int printView(View v) {

        return printBitmap(BitmapUtils.drawToBitmap(v, 600, 800));
    }


    private boolean isInitPrint = false;

    @Override
    public int printBitmap(Bitmap bm) {

        final int printerWidth = 600;
//        final int printerWidth = bm.getWidth();

        LabelCommand tsc = new LabelCommand();

        if (!isInitPrint) {

            isInitPrint = true;

            tsc.addSize(75, 100); // Set the label size, set according to the actual size

            tsc.addGap(0);           // Set the label gap, set according to the actual size, if it is no gap paper, set it to 0

            tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL); // set print orientation

            tsc.addReference(0, 0); // set origin coordinates

            tsc.addTear(EscCommand.ENABLE.ON); // tear off mode on
        }

        tsc.addCls(); // clear print buffer

        // draw pictures
        tsc.addBitmap(0, 0, LabelCommand.BITMAP_MODE.OR, printerWidth, bm);

//        tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.gprinter.com.cn");
        // Draw 1D barcode
//        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "Gprinter");

        tsc.addPrint(1, 1); // print labels

        tsc.addSound(2, 100); // buzzer sounds after printing label

        Vector<Byte> datas = tsc.getCommand(); // send data
        Byte[] Bytes = datas.toArray(new Byte[0]);
        byte[] bytes = ArrayUtils.toPrimitive(Bytes);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);

        try {
            return mGpService.sendLabelCommand(mPickPrinter, str);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return -1;
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int printBitmap(Bitmap bm, Point point, @NonNull Size size, int printWidth) {

        LabelCommand tsc = new LabelCommand();

        if (!isInitPrint) {

            isInitPrint = true;

            tsc.addSize(size.getWidth(), size.getHeight()); // Set the label size, set according to the actual size

            tsc.addGap(2);           // Set the label gap, set according to the actual size, if it is no gap paper, set it to 0

            tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL); // set print orientation

            tsc.addReference(point.x, point.y); // set origin coordinates

            tsc.addTear(EscCommand.ENABLE.ON); // tear off mode on
        }

        tsc.addCls(); // clear print buffer
        // draw pictures
        tsc.addBitmap(0, 0, LabelCommand.BITMAP_MODE.OR, printWidth * 8, bm);

        tsc.addPrint(1, 1); // print labels

        tsc.addSound(2, 100); // buzzer sounds after printing label

        Vector<Byte> datas = tsc.getCommand(); // send data
        Byte[] Bytes = datas.toArray(new Byte[0]);
        byte[] bytes = ArrayUtils.toPrimitive(Bytes);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);

        try {
            return mGpService.sendLabelCommand(mPickPrinter, str);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return -1;
    }


    @Override
    public int openPort(int printerId, String deviceMac) {

        try {
            return mGpService.openPort(printerId, PrinterService.PORT_TYPE_BLUETOOTH, deviceMac, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public void closePort(int PrinterId) {

        try {
            mGpService.closePort(PrinterId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getPrinterConnectStatus(int PrinterId) {

        try {
            return mGpService.getPrinterConnectStatus(PrinterId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public int printeTestPage(int PrinterId) {

        try {
            return mGpService.printeTestPage(PrinterId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public void queryPrinterStatus(int PrinterId, int Timesout, int requestCode) {
        try {
            mGpService.queryPrinterStatus(PrinterId, Timesout, requestCode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPrinterCommandType(int PrinterId) {

        try {
            return mGpService.getPrinterCommandType(PrinterId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;

    }

    @Override
    public int sendEscCommand(int PrinterId, String b64) {
        try {
            return mGpService.sendEscCommand(PrinterId, b64);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public int sendLabelCommand(int PrinterId, String b64) {
        try {
            return mGpService.sendLabelCommand(PrinterId, b64);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public void isUserExperience(boolean userExperience) {
        try {
            mGpService.isUserExperience(userExperience);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getClientID() {

        try {
            return mGpService.getClientID();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int setServerIP(String ip, int port) {

        try {

            return mGpService.setServerIP(ip, port);

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * determine if Bluetooth is supported and enable Bluetooth
     * After receiving the Bluetooth adapter, determine if Bluetooth is supported and if Bluetooth is enabled.
     * If it is not enabled, the user needs to enable Bluetooth
     */
    public boolean checkBleDevice() {
        Log.i("!!!", "check");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
                return true;
            }
        } else {
            Log.i("blueTooth", "Phone does not support Bluetooth");
        }
        return false;
    }

}
