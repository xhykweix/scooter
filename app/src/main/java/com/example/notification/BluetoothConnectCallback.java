package com.example.notification;

import tool.ConnectThread;

public interface BluetoothConnectCallback
{
    public static final int nobind=-1;
    public static final int noSearch=-2;
    public static final int bluetoothNoSupport=-3;
    public abstract void onSuccess(ConnectThread connectThread);
    public abstract void onFailure(int code);
}
