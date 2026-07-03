package com.example.musio.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.musio.data.FirestoreRepository;
import com.example.musio.utils.AppUtils;
import com.example.musio.utils.NotificationUtils;

public class PlaylistCleanupReceiver extends BroadcastReceiver {

    private static final String TAG = "PlaylistCleanupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Playlist cleanup triggered.");
        clearPlaylist(context);
    }

    private void clearPlaylist(Context context) {
        FirestoreRepository firestoreRepository = new FirestoreRepository();
        firestoreRepository.clearPlaylist(new FirestoreRepository.ClearPlaylistCallback() {
            @Override
            public void onClearSuccess() {
                Log.d(TAG, "Playlist cleared.");
                notifyUser(context); // Notify the user after the deletion
            }

            @Override
            public void onClearFailure(Exception e) {
                Log.e(TAG, "Error clearing playlist: ", e);
            }
        });
    }

    private void notifyUser(Context context) {
        if (!AppUtils.isAppInForeground(context)) {
            NotificationUtils.showNotification(context, "Playlist mise à jour", "La nouvelle playlist collaborative est disponible !");
        } else {
            Log.d(TAG, "L'application est en premier plan, aucune notification n'est envoyée.");
        }
    }
}