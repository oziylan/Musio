/*
from Android Developers documentation :
https://developer.android.com/develop/ui/views/notifications/channels#java
 */

package com.example.musio.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.musio.R;
import com.example.musio.MainActivity;

public class NotificationUtils {

    private static final String CHANNEL_ID = "MUSIO_CHANNEL_ID";
    private static final String CHANNEL_NAME = "Musio Notifications";
    private static final String CHANNEL_DESC = "Notifications for Musio app";
    private static int notificationId = 0;

    //from Android Developers documentation
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("NotificationUtils", "Canal de notification créé");
            } else {
                Log.e("NotificationUtils", "NotificationManager est null");
            }
        }
    }
    //from Android Developers documentation
    public static void showNotification(Context context, String title, String message) {
        Log.d("NotificationUtils", "Tentative d'affichage de la notification : " + title);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId++, builder.build());
            Log.d("NotificationUtils", "Notification affichée avec succès");
        } else {
            Log.e("NotificationUtils", "NotificationManager est null");
        }
    }
}