package com.example.notification;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tool.ConnectThread;
import tool.MyBluetoothService;
import tool.NotificationCatchForGoogleMap;

public class MainService extends Service
{
    public static final int connect = 1;
    public static final int disconnect = 0;
    public static final int connecting = 3;
    private MediaPlayer mp;
    private static ConnectThread connectThread;
    private int count = 0;
    private Bundle message = new Bundle();
    private Intent intent = new Intent("MainService");
    private Timer timer = new Timer();
    private TimerTask task;
    private boolean shutDown;
    @Override
    public void onCreate()
    {
        super.onCreate();
        message.putInt("connectStatus", connecting);
        intent.putExtras(message);
        sendBroadcast(intent);

        BluetoothConnectThread bluetoothConnectThread = new BluetoothConnectThread(getApplicationContext(), new BluetoothConnectCallback()
        {

            @Override
            public void onSuccess(ConnectThread connectThread)
            {
                MainService.connectThread = connectThread;
                message.putInt("connectStatus", connect);
                intent.putExtras(message);
                sendBroadcast(intent);
                MyBluetoothService myBluetoothService = new MyBluetoothService(connectThread);
                myBluetoothService.enableReadData();
                NotificationCatchForGoogleMap.setDirCentimeter(getApplicationContext().getSharedPreferences("autoTurnSignal", MODE_PRIVATE).getInt("dirCentimeter",200));
                NotificationMonitorService.sentStatus = true;
                task = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        Boolean temp = myBluetoothService.isConnected();
                        Log.e("run", temp.toString());
                        if (!myBluetoothService.isConnected() || shutDown)
                        {
                            NotificationMonitorService.sentStatus = false;
                            connectThread.cancel();
                            timer.cancel();
                            task.cancel();
                            stopSelf();
                        }
                        else if(!shutDown)
                        {
                            message.putInt("connectStatus", connect);
                            intent.putExtras(message);
                            sendBroadcast(intent);
                        }

                        isNotificationMonitorService();
                    }
                };
                timer.schedule(task, 5000, 1000);
            }

            @Override
            public void onFailure(int code)
            {
                if (code == BluetoothConnectCallback.noSearch)
                {
                    message.putInt("connectStatus", BluetoothConnectCallback.noSearch);
                    intent.putExtras(message);
                    sendBroadcast(intent);
                }
                else if (code == BluetoothConnectCallback.nobind)
                {
                    message.putInt("connectStatus", BluetoothConnectCallback.nobind);
                    intent.putExtras(message);
                    sendBroadcast(intent);
                }
                else if(code==BluetoothConnectCallback.bluetoothNoSupport)
                {
                    message.putInt("connectStatus", BluetoothConnectCallback.bluetoothNoSupport);
                    intent.putExtras(message);
                    sendBroadcast(intent);
                }
                stopSelf();
            }
        });
        bluetoothConnectThread.start();
        Log.e("Service", "onCreate");
    }


    private void isNotificationMonitorService()
    {
        // 檢查通知欄擷取是否失效，
        // 如果失效 將呼叫 restartNotificationMonitorService() 重新啟動 通知欄擷取
        // 如果未失效 將不動作
        ComponentName componentName = new ComponentName(this, NotificationMonitorService.class);
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServiceInfo = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfo == null)
            return;
        for (ActivityManager.RunningServiceInfo service : runningServiceInfo)
            if (service.service.equals(componentName))
                isRunning = true;
        if (isRunning)
            return;
        restartNotificationMonitorService();
    }

    private void restartNotificationMonitorService()
    { // 重新啟動 通知欄擷取
        ComponentName componentName = new ComponentName(this, NotificationMonitorService.class);
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e("Service", "onStartCommand");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel("service start", "背景服務通知", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }


        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplication(), 0, activityIntent, 0);
        Notification.Builder notificationBuilder = new Notification.Builder(getApplication())
                .setSmallIcon(R.drawable.scooter_icon)
                .setTicker("程式開始在背景執行，並且已連線到機車")
                .setContentTitle("背景執行中")
                .setContentText("已連線到機車")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationBuilder.setChannelId("service start");
        Notification notification = notificationBuilder.build();

        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.e("Service", "onBind");
        return null;
    }

    @Override
    public void onDestroy()
    {
        shutDown=true;
        super.onDestroy();
        NotificationMonitorService.sentStatus = false;
        timer.cancel();
        if (task != null)
            task.cancel();
        if(connectThread!=null)
            connectThread.cancel();
        //mp.stop();
        message.putInt("connectStatus", disconnect);
        intent.putExtras(message);
        sendBroadcast(intent);
        Log.e("Service", "STOP");
    }

    public static ConnectThread getConnectThread()
    {
        return connectThread;
    }
}
