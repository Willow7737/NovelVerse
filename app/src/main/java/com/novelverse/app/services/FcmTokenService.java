package com.novelverse.app.services;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * FcmTokenService — handles FCM token refresh events.
 *
 * Firebase registers this alongside FcmService. Token management (e.g. sending
 * the new token to Supabase) is delegated to FcmService.onNewToken, so this class
 * acts as a lightweight secondary registration point required by older GMS versions.
 *
 * If your minSdk is 24+ with modern Play Services, a single FcmService is enough —
 * this stub ensures the Manifest entry doesn't cause a ClassNotFoundException crash
 * on devices that resolve service names eagerly.
 */
public class FcmTokenService extends FirebaseMessagingService {

    private static final String TAG = "FcmTokenService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Delegate to the main FcmService logic to avoid duplication.
        Log.d(TAG, "onNewToken received — forwarding to FcmService handler");
        // FcmService.sendTokenToServer is package-private; trigger via Application
        // or shared preferences so the next app launch picks up the new token.
        try {
            getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("pending_token", token)
                    .putBoolean("token_needs_sync", true)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist pending FCM token", e);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // All message handling is done in FcmService — this service only handles token refresh.
        Log.d(TAG, "onMessageReceived in FcmTokenService (no-op — handled by FcmService)");
    }
}
