package com.example.notification;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Set;

import tool.ConnectThread;

import static android.content.Context.MODE_PRIVATE;
import static com.example.notification.MainService.connect;

public class BluetoothConnectThread extends Thread
{
    private SharedPreferences sharedPreferences;
    private String pairMAC;
    private BluetoothConnectCallback callback;
    private ConnectThread connectThread;
    private BluetoothSocket mmSocket = null;
    private boolean checkBind=true;

    public BluetoothConnectThread(Context context, BluetoothConnectCallback callback,boolean checkBind)
    {
        this.callback = callback;
        this.checkBind=checkBind;
        sharedPreferences = context.getSharedPreferences("Bluetooth", MODE_PRIVATE);
        pairMAC = sharedPreferences.getString("PairMAC", "noPair");
    }

    public BluetoothConnectThread(Context context, BluetoothConnectCallback callback)
    {
        this.callback = callback;
        sharedPreferences = context.getSharedPreferences("Bluetooth", MODE_PRIVATE);
        pairMAC = sharedPreferences.getString("PairMAC", "noPair");
    }

    @Override
    public void run()
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (bluetoothAdapter == null)
        {
            callback.onFailure(BluetoothConnectCallback.bluetoothNoSupport);
        }
        else if (pairedDevices.size() > 0)
        {
            boolean pair = false;
            if (pairMAC.equals("noPair"))
            {
                callback.onFailure(BluetoothConnectCallback.nobind);
                return;
            }

            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices)
            {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                if (pairMAC.equals(deviceHardwareAddress))
                {
                    pair = true;
                    /*for (ParcelUuid i : device.getUuids())
                        Log.d("Bluetooth", i.toString());*/
                    Log.d("Bluetooth", deviceHardwareAddress);
                    connectThread = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceHardwareAddress));
                    mmSocket = connectThread.getMmSocket();
                    connectThread.run();

                    if (mmSocket.isConnected())
                        callback.onSuccess(connectThread);
                    else
                        callback.onFailure(BluetoothConnectCallback.noSearch);
                    break;
                }
            }

            if (!pair)
                callback.onFailure(BluetoothConnectCallback.nobind);
        }
        else
            callback.onFailure(BluetoothConnectCallback.nobind);
    }
}
