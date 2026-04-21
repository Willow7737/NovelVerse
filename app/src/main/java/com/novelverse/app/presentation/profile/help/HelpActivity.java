package com.novelverse.app.presentation.profile.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.novelverse.app.ui.banner.BannerHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Help & Support — FAQ items + contact channels.
 */
@AndroidEntryPoint
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // FAQ items
        findViewById(R.id.faq_reading).setOnClickListener(v ->
                toast("You can download any chapter from the chapter menu while online. Downloaded content appears here."));
        findViewById(R.id.faq_subscription).setOnClickListener(v ->
                toast("Manage your subscription from Settings → Subscription. Cancel anytime."));
        findViewById(R.id.faq_account).setOnClickListener(v ->
                toast("Use Forgot Password on the login screen to reset. For other issues, email support."));
        findViewById(R.id.faq_points).setOnClickListener(v ->
                toast("Earn points by reading daily and completing streaks. Spend them to unlock premium chapters."));

        // Contact
        findViewById(R.id.contact_email).setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:support@novelverse.app?subject=Support Request"));
            if (email.resolveActivity(getPackageManager()) != null) startActivity(email);
            else toast("No email app found.");
        });

        findViewById(R.id.contact_report).setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:support@novelverse.app?subject=Bug Report"));
            if (email.resolveActivity(getPackageManager()) != null) startActivity(email);
            else toast("No email app found.");
        });
    }

    private void toast(String msg) {
        BannerHelper.info(this, msg);
    }
}
