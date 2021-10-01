package com.example.notification;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BleCallBack extends Service implements LeScanCallback
{

    private final static String TAG = BleCallBack.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    private boolean shouldScan = true;

    private TimerTask task;

    private Timer timer=new Timer();

    static int connectState;

    private LeCallBack leCallBack;

    public class LocalBinder extends Binder
    {
        public BleCallBack getService()
        {
            return BleCallBack.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // 處理 Service 傳來的訊息。
                Bundle message = intent.getExtras();
                int status = message.getInt("connectStatus");

                if (status == MainService.connect)
                    connectState=MainService.connect;
                else if(status == BluetoothConnectCallback.nobind)
                    stopSelf();
                else
                    connectState=MainService.disconnect;
            }
        };

        IntentFilter filter = new IntentFilter("MainService");
        // 將 BroadcastReceiver 在 Activity 掛起來。
        registerReceiver(receiver, filter);
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        startScan();
        task = new TimerTask()
        {
            @Override
            public void run()
            {
                stopScan();
                if(isReady())
                    startScan();
            }
        };
        timer.schedule(task, 500, 60000);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Initializes a reference to the local bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize()
    {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
            {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        if (mBluetoothAdapter == null)
        {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null)
            {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
        }

        if(leCallBack==null)
            leCallBack=new LeCallBack(this);

        Log.d(TAG, "Initialzed scanner.");
        return true;
    }

    /**
     * Checks if bluetooth is correctly set up.
     *
     * @return
     */
    protected boolean isInitialized()
    {
        return mBluetoothManager != null && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    /**
     * Checks if ble is ready and bluetooth is correctly setup.
     *
     * @return
     */
    protected boolean isReady()
    {
        return isInitialized() && isBleReady();
    }

    /**
     * Checks if the device is ble ready.
     *
     * @return
     */
    protected boolean isBleReady()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        Log.d(TAG, "Found ble device" + device.getName() + "" + device.getAddress() + " rssi:" + rssi);
        broadcastOnDeviceFound(device, scanRecord);
    }

    /**
     * Broadcasts a message with the given device.
     *
     * @param device
     * @param scanRecord
     */
    protected void broadcastOnDeviceFound(final BluetoothDevice device, byte[] scanRecord)
    {
        assert device != null : "Device should not be null.";

        Log.i(TAG, "DriveFound");
    }

    /**
     * Starts the bluetooth low energy scan It scans at least the
     * delayStopTimeInMillis.
     *
     * @param delayStopTimeInMillis the duration of the scan
     * @return <wyn>true</wyn> if the scan is successfully started.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean startScan(long delayStopTimeInMillis)
    {
        if (!isReady())
            return false;

        if (shouldScan)
        {
            if (delayStopTimeInMillis <= 0)
            {
                Log.w(TAG, "Did not start scanning with automatic stop delay time of" + delayStopTimeInMillis);
                return false;
            }

            Log.d(TAG, "Auto-Stop scan after" + delayStopTimeInMillis + " ms");
            getMainHandler().postDelayed(new Runnable()
            {

                @Override
                public void run()
                {
                    stopScan();
                }
            }, delayStopTimeInMillis);
        }
        return startScan();
    }

    /**
     * @return an handler with the main (ui) looper.
     */
    private Handler getMainHandler()
    {
        return new Handler(getMainLooper());
    }

    /**
     * Starts the bluetooth low energy scan. It scans without time limit.
     *
     * @return <wyn>true</wyn> if the scan is successfully started.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean startScan()
    {
        if (!isReady())
            return false;

        if (shouldScan)
        {
            if (mBluetoothAdapter != null)
            {
                Log.d(TAG, "Started scan.");
                mBluetoothAdapter.getBluetoothLeScanner().startScan(filter(),settings(),leCallBack);
                return true;
            }
            else
            {
                Log.d(TAG, "BluetoothAdapter is null.");
                return false;
            }
        }
        return false;
    }

    private List<ScanFilter> filter()
    {
        ArrayList<ScanFilter>temp=new ArrayList<ScanFilter>();
        temp.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString("12634d89-d598-4874-8e86-7d042ee07ba7"))).build());
        return temp;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private ScanSettings settings()
    {
        return new ScanSettings.Builder().setMatchMode(ScanSettings.CALLBACK_TYPE_MATCH_LOST).build();
    }

    /**
     * Stops the bluetooth low energy scan.
     */
    public void stopScan()
    {
        if (mBluetoothAdapter != null)
        {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(leCallBack);
            Log.d(TAG, "Stopped scan.");
        }
        else
        {
            Log.d(TAG, "BluetoothAdapter is null.");
        }
    }

    @Override
    public void onDestroy()
    {
        shouldScan = false;
        stopScan();
        super.onDestroy();
    }
}

class LeCallBack extends ScanCallback
{
    BleCallBack bleCallBack;
    BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
    LeCallBack(BleCallBack bleCallBack)
    {
        this.bleCallBack=bleCallBack;
    }
    @Override
    public void onScanResult(int callbackType, ScanResult result)
    {
        BluetoothDevice device = result.getDevice();
        Log.d("bleCallback", "Found ble device" + device.getName() + " " + device.getAddress() + " rssi:" + result.getRssi()+" "+BleCallBack.connectState);

        if(BleCallBack.connectState==MainService.disconnect && bluetoothAdapter.isEnabled())
        {
            Intent intent = new Intent("tt");
            intent.setPackage(bleCallBack.getPackageName());
            bleCallBack.startService(intent);
        }

    }

    @Override
    public void onBatchScanResults(List<ScanResult> results)
    {
        super.onBatchScanResults(results);
        for (ScanResult i : results)
        {
            BluetoothDevice device = i.getDevice();
            Log.d("bleCallbackList", "Found ble device" + device.getName() + " " + device.getAddress() + " rssi:" + i.getRssi()+" "+BleCallBack.connectState);
        }
    }

    //當掃描不能開啟時回撥
    @Override
    public void onScanFailed(int errorCode)
    {
        super.onScanFailed(errorCode);
        //掃描太頻繁會返回ScanCallback.SCAN_FAILED_ALREADY_STARTED，表示app無法註冊，無法開始掃描。
        switch (errorCode)
        {
            case SCAN_FAILED_ALREADY_STARTED:
                Log.d("bleCallback", "SCAN_FAILED_ALREADY_STARTED");
        }


    }

}
