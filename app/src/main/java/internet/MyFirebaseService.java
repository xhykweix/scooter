package internet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.notification.MainActivity;
import com.example.notification.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import tool.NewListDataSQL;


public class MyFirebaseService extends FirebaseMessagingService
{
    NewListDataSQL helper = new NewListDataSQL(this, "time");
    /*@Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
       // super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null)
        {
            Log.i("MyFirebaseService","title "+remoteMessage.getNotification().getTitle());
            Log.i("MyFirebaseService","body "+remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());
        }

    }*/

    @Override
    public void onNewToken(String s)
    {
        super.onNewToken(s);
        Log.i("MyFirebaseService", "token " + s);
    }

    private void sendNotification(String messageTitle, String messageBody)
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "default_notification_channel_id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle(messageTitle)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }

    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        if (remoteMessage.getData().size() > 0)
        {
            Map<String, String> receivedMap = remoteMessage.getData();

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_ONE_SHOT);

            String channelId = "機車移動通知";
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setContentTitle(receivedMap.get("title"))
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentText(receivedMap.get("body") + " 觸發時間: " + receivedMap.get("time"))
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "機車移動通知",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }

            //save message
            helper.addData(helper.getReadableDatabase(),"time",receivedMap.get("time"));
            notificationManager.notify(0, notificationBuilder.build());
        }
    }
}
