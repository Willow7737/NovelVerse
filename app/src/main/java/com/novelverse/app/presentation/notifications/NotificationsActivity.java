package com.novelverse.app.presentation.notifications;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Notifications Activity — launched from the header bell icon.
 * Currently shows an empty-state placeholder.
 * Wire Supabase Realtime channel here when backend notifications are live.
 */
@AndroidEntryPoint
public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
