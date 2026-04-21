package com.novelverse.app.presentation.profile.downloads;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;
import com.novelverse.app.presentation.home.HomeActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Downloads — lists novels saved for offline reading.
 * Currently shows empty state; wire to DownloadService when ready.
 */
@AndroidEntryPoint
public class DownloadsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Browse button → go to home
        findViewById(R.id.btn_browse).setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }
}
