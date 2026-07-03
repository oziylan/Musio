/*
from Documentation Android Developers :
https://developer.android.com/develop/background-work/services
https://developer.android.com/reference/java/util/Calendar

from ChatGPT
 */
package com.example.musio.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import java.util.Calendar;

public class PlaylistService extends Service {

    private static final String TAG = "PlaylistService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Schedule record deletion and daily notification
        scheduleDailyCleanup();
        return START_STICKY;
    }

    private void scheduleDailyCleanup() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PlaylistCleanupReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Schedule for 8am every day
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed for today, plan for the next day
        if (Calendar.getInstance().after(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);

        Log.d(TAG, "Daily cleanup and notification scheduled.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PlaylistService destroyed.");
        stopService(new Intent(this, PlaylistService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}