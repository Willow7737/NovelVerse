package com.novelverse.app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.novelverse.app.NovelVerseApplication;
import com.novelverse.app.R;
import com.novelverse.app.presentation.home.HomeActivity;

import java.util.Map;

/**
 * Service for Firebase Cloud Messaging
 */
public class FcmService extends FirebaseMessagingService {

    private static final String TAG = "FcmService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        
        // Send token to server
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        
        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }
        
        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }

    /**
     * Handle data message
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        
        if (type == null) return;
        
        switch (type) {
            case "new_chapter":
                // Handle new chapter notification
                String novelId = data.get("novel_id");
                String chapterId = data.get("chapter_id");
                showNotification(
                    "New Chapter",
                    data.get("message"),
                    data
                );
                break;
                
            case "comment_reply":
                // Handle comment reply
                showNotification(
                    "New Reply",
                    data.get("message"),
                    data
                );
                break;
                
            case "author_follow":
                // Handle author follow notification
                showNotification(
                    data.get("title"),
                    data.get("message"),
                    data
                );
                break;
                
            default:
                // Handle generic notification
                showNotification(
                    data.get("title"),
                    data.get("message"),
                    data
                );
                break;
        }
    }

    /**
     * Show notification
     */
    private void showNotification(String title, String message, Map<String, String> data) {
        String channelId = NovelVerseApplication.CHANNEL_ID_GENERAL;
        
        // Determine channel based on notification type
        String type = data.get("type");
        if ("new_chapter".equals(type)) {
            channelId = NovelVerseApplication.CHANNEL_ID_NEW_CHAPTERS;
        } else if ("comment_reply".equals(type) || "author_follow".equals(type)) {
            channelId = NovelVerseApplication.CHANNEL_ID_SOCIAL;
        }
        
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add extras from data
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Send token to server
     */
    private void sendTokenToServer(String token) {
        // Send FCM token to server for user association
    }
}
