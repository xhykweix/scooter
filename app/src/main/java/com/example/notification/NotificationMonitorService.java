package com.example.notification;

import android.app.Notification;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.annotation.RequiresApi;
import tool.NotificationCatchForGoogleMap;

public class NotificationMonitorService extends NotificationListenerService {
    static boolean sentStatus;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) { //通知出現將觸發
        Bundle extras = sbn.getNotification().extras;
        String packageName = sbn.getPackageName(); // 取得應用程式包名

        if (packageName.equals("com.google.android.apps.maps") && sentStatus) {//判斷只接收google map通知{

            String title = extras.getString(Notification.EXTRA_TITLE); // 取得通知欄標題
            String text = extras.getString(Notification.EXTRA_TEXT); // 取得通知欄文字
            Icon largeIcon = sbn.getNotification().getLargeIcon();
           /* try { // 取得通知欄的小圖示
                PackageManager manager = getPackageManager();
                Resources resources = manager.getResourcesForApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }*/
            NotificationCatchForGoogleMap.show(packageName, title, text, largeIcon,this);//傳送資料
        }
    }
}
