package com.novelverse.app.crash;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        TextView crashLog = findViewById(R.id.crashLog);
        Button copyBtn = findViewById(R.id.copyButton);
        Button exitBtn = findViewById(R.id.exitButton);

        String log = getIntent().getStringExtra("crash_log");

        if (log == null) {
            log = "No crash log available";
        }

        crashLog.setText(log);

        String finalLog = log;

        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("Crash Log", finalLog);
            clipboard.setPrimaryClip(clip);
        });

        exitBtn.setOnClickListener(v -> finishAffinity());
    }
}