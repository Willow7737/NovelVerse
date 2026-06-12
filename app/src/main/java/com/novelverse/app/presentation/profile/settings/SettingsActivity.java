package com.novelverse.app.presentation.profile.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.novelverse.app.R;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.presentation.common.legal.LegalActivity;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.payment.store.PointStoreActivity;
import com.novelverse.app.presentation.payment.subscriptions.SubscriptionActivity;
import com.novelverse.app.presentation.profile.downloads.DownloadsActivity;
import com.novelverse.app.presentation.profile.edit.EditProfileActivity;
import com.novelverse.app.presentation.profile.help.HelpActivity;
import com.novelverse.app.presentation.profile.reading.ReadingPrefsActivity;
import com.novelverse.app.ui.banner.BannerHelper;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Account
        bindRow(R.id.row_edit_profile,    R.drawable.ic_edit,        "Edit Profile",       null,
                () -> go(EditProfileActivity.class));
        bindRow(R.id.row_change_password, R.drawable.ic_lock,        "Change Password",    null,
                this::showChangePasswordSheet);
        bindRow(R.id.row_subscription,    R.drawable.ic_crown,       "Subscription",       "Free",
                () -> go(SubscriptionActivity.class));
        bindRow(R.id.row_point_store,     R.drawable.ic_coins,       "Point Store",        null,
                () -> go(PointStoreActivity.class));

        // Reading
        bindRow(R.id.row_reading_prefs,   R.drawable.ic_font,        "Reading Preferences", null,
                () -> go(ReadingPrefsActivity.class));
        bindRow(R.id.row_downloads,       R.drawable.ic_download,    "Downloads",          null,
                () -> go(DownloadsActivity.class));

        // Notifications toggles — use SwitchMaterial (matches the XML widget type)
        bindToggle(R.id.row_push_notifs,  R.drawable.ic_notification,"Push Notifications", true);
        bindToggle(R.id.row_email_notifs, R.drawable.ic_document,    "Email Notifications",true);
        bindToggle(R.id.row_marketing,    R.drawable.ic_tag,         "Marketing Emails",   false);

        // Privacy
        bindRow(R.id.row_privacy,         R.drawable.ic_shield,      "Privacy",            "Public",
                this::showPrivacySheet);
        bindRow(R.id.row_export_data,     R.drawable.ic_download,    "Export My Data",     null,
                this::exportData);
        bindRow(R.id.row_delete_account,  R.drawable.ic_delete,      "Delete Account",     null,
                this::confirmDeleteAccount);

        // App
        bindRow(R.id.row_language,        R.drawable.ic_globe,       "Language",           "English",
                this::showLanguageSheet);
        bindRow(R.id.row_help,            R.drawable.ic_question,    "Help & Support",     null,
                () -> go(HelpActivity.class));
        bindRow(R.id.row_legal,           R.drawable.ic_document,    "Privacy & Terms",    null,
                () -> go(LegalActivity.class));
        bindRow(R.id.row_about,           R.drawable.ic_star,        "About NovelVerse",   "v1.0", () ->
                BannerHelper.info(this, "NovelVerse v1.0",
                        "Made with love by Spidroid Technologies."));

        // Sign out
        findViewById(R.id.btn_sign_out).setOnClickListener(v -> confirmSignOut());
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void showChangePasswordSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        // background handled by BottomSheetTheme
        root.setPadding(dp(24), dp(16), dp(24), dp(36));

        addSheetHandle(root);
        addSheetTitle(root, "Change Password");

        android.widget.EditText current = new android.widget.EditText(this);
        current.setHint("Current password"); current.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        current.setPadding(dp(12), dp(12), dp(12), dp(12));
        styleInput(current);
        android.widget.LinearLayout.LayoutParams lp1 = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp1.bottomMargin = dp(12); current.setLayoutParams(lp1);
        root.addView(current);

        android.widget.EditText newPass = new android.widget.EditText(this);
        newPass.setHint("New password"); newPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPass.setPadding(dp(12), dp(12), dp(12), dp(12));
        styleInput(newPass);
        android.widget.LinearLayout.LayoutParams lp2 = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp2.bottomMargin = dp(24); newPass.setLayoutParams(lp2);
        root.addView(newPass);

        android.widget.TextView btn = makeSheetBtn("Update Password",
            android.graphics.Color.WHITE, android.graphics.Color.parseColor("#0085FF"));
        btn.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        btn.setOnClickListener(v -> {
            String np = newPass.getText().toString().trim();
            if (np.length() < 8) {
                BannerHelper.warning(this, "Too short", "Password must be at least 8 characters.");
                return;
            }
            authViewModel.changePassword(newPass.getText().toString(), (success, error) ->
                runOnUiThread(() -> {
                    if (success) {
                        BannerHelper.success(this, "Password updated!");
                        sheet.dismiss();
                    } else {
                        BannerHelper.error(this, "Update failed", error != null ? error : "Try again.");
                    }
                }));
        });
        root.addView(btn);
        sheet.setContentView(root);
        sheet.show();
    }

    private void showPrivacySheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        // background handled by BottomSheetTheme
        root.setPadding(dp(24), dp(16), dp(24), dp(36));

        addSheetHandle(root);
        addSheetTitle(root, "Privacy Settings");

        String[] options = {"Public", "Friends Only", "Private"};
        for (String opt : options) {
            android.widget.TextView item = new android.widget.TextView(this);
            item.setText(opt); item.setTextSize(17f);
            item.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            item.setPadding(dp(4), dp(16), dp(4), dp(16));
            item.setOnClickListener(v -> {
                BannerHelper.success(this, "Privacy updated", "Set to " + opt);
                sheet.dismiss();
            });
            root.addView(item);
            android.view.View div = new android.view.View(this);
            div.setBackgroundColor(0xFFE2E8F0);
            div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
            root.addView(div);
        }
        sheet.setContentView(root);
        sheet.show();
    }

    private void exportData() {
        BannerHelper.info(this, "Export requested",
                "We'll email you a copy of your data within 48 hours.");
    }

    private void confirmDeleteAccount() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        // background handled by BottomSheetTheme
        root.setPadding(dp(24), dp(16), dp(24), dp(36));
        addSheetHandle(root);
        addSheetTitle(root, "Delete Account?");

        android.widget.TextView body = new android.widget.TextView(this);
        body.setText("This will permanently delete your account, novels, and all data. This cannot be undone.");
        body.setTextColor(resolveAttrColor(android.R.attr.textColorSecondary)); body.setTextSize(15f);
        body.setGravity(android.view.Gravity.CENTER); body.setLineSpacing(0f, 1.5f);
        android.widget.LinearLayout.LayoutParams bLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.bottomMargin = dp(24); body.setLayoutParams(bLp); root.addView(body);

        android.widget.TextView del = makeSheetBtn("Delete My Account",
            android.graphics.Color.parseColor("#EF4444"), android.graphics.Color.parseColor("#FEE2E2"));
        android.widget.LinearLayout.LayoutParams dLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        dLp.bottomMargin = dp(12); del.setLayoutParams(dLp);
        del.setOnClickListener(v -> {
            sheet.dismiss();
            BannerHelper.info(this, "Contact support", "Email support@novelverse.app to proceed with account deletion.");
        });
        root.addView(del);

        android.widget.TextView cancel = makeSheetBtn("Keep My Account",
            resolveAttrColor(android.R.attr.textColorSecondary), resolveAttrColor(R.attr.colorButtonSecondaryBg));
        cancel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        cancel.setOnClickListener(v -> sheet.dismiss()); root.addView(cancel);
        sheet.setContentView(root); sheet.show();
    }

    private void showLanguageSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        // background handled by BottomSheetTheme
        root.setPadding(dp(24), dp(16), dp(24), dp(36));
        addSheetHandle(root);
        addSheetTitle(root, "Language");

        String[] langs = {"English", "Spanish", "French", "German", "Portuguese",
                          "Japanese", "Korean", "Chinese (Simplified)"};
        for (String lang : langs) {
            android.widget.TextView item = new android.widget.TextView(this);
            item.setText(lang); item.setTextSize(17f);
            item.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            item.setPadding(dp(4), dp(16), dp(4), dp(16));
            item.setOnClickListener(v -> {
                BannerHelper.success(this, "Language set", "App language changed to " + lang);
                sheet.dismiss();
            });
            root.addView(item);
            android.view.View div = new android.view.View(this);
            div.setBackgroundColor(0xFFE2E8F0);
            div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
            root.addView(div);
        }
        sheet.setContentView(root); sheet.show();
    }

    private void confirmSignOut() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        // background handled by BottomSheetTheme
        root.setPadding(dp(24), dp(16), dp(24), dp(36));
        addSheetHandle(root);
        addSheetTitle(root, "Sign out?");

        android.widget.TextView out = makeSheetBtn("Sign Out",
            android.graphics.Color.parseColor("#EF4444"), android.graphics.Color.parseColor("#FEE2E2"));
        android.widget.LinearLayout.LayoutParams oLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        oLp.bottomMargin = dp(12); out.setLayoutParams(oLp);
        out.setOnClickListener(v -> {
            sheet.dismiss();
            authViewModel.signOut((success, error) -> {
                if (success) {
                    Intent i = new Intent(this, OnboardingActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                } else {
                    BannerHelper.error(this, "Sign out failed");
                }
            });
        });
        root.addView(out);

        android.widget.TextView cancel = makeSheetBtn("Cancel",
            resolveAttrColor(android.R.attr.textColorSecondary), resolveAttrColor(R.attr.colorButtonSecondaryBg));
        cancel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        cancel.setOnClickListener(v -> sheet.dismiss()); root.addView(cancel);
        sheet.setContentView(root); sheet.show();
    }

    // ── Sheet helpers ─────────────────────────────────────────────────────────

    private void addSheetHandle(android.widget.LinearLayout root) {
        android.view.View h = new android.view.View(this);
        android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
        hBg.setColor(resolveAttrColor(R.attr.colorDragHandle)); hBg.setCornerRadius(dp(100));
        h.setBackground(hBg);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(dp(40), dp(4));
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL; lp.bottomMargin = dp(20);
        h.setLayoutParams(lp); root.addView(h);
    }

    private void addSheetTitle(android.widget.LinearLayout root, String text) {
        android.widget.TextView t = new android.widget.TextView(this);
        t.setText(text); t.setTextSize(20f);
        t.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(20); t.setLayoutParams(lp); root.addView(t);
    }

    private android.widget.TextView makeSheetBtn(String label, int textColor, int bgColor) {
        android.widget.TextView btn = new android.widget.TextView(this);
        btn.setText(label); btn.setTextColor(textColor); btn.setTextSize(16f);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(100)); btn.setBackground(bg);
        return btn;
    }

    private void styleInput(android.widget.EditText et) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(resolveAttrColor(R.attr.colorSurfaceCardTertiary));
        bg.setStroke(1, resolveAttrColor(R.attr.colorSurfaceCardStroke));
        bg.setCornerRadius(dp(10)); et.setBackground(bg);
    }

    // ── Row binders ───────────────────────────────────────────────────────────

    private void bindRow(int rowId, int iconRes, String label, String value, Runnable onClick) {
        android.view.View row = findViewById(rowId);
        if (row == null) return;
        ((android.widget.ImageView) row.findViewById(R.id.row_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        TextView valView = row.findViewById(R.id.row_value);
        if (value != null) valView.setText(value);
        else valView.setVisibility(android.view.View.GONE);
        row.setOnClickListener(v -> onClick.run());
    }

    private void bindToggle(int rowId, int iconRes, String label, boolean defaultOn) {
        android.view.View row = findViewById(rowId);
        if (row == null) return;
        ((android.widget.ImageView) row.findViewById(R.id.row_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        // FIX: cast to SwitchMaterial to match the XML widget type
        SwitchMaterial toggle = row.findViewById(R.id.row_toggle);
        if (toggle != null) toggle.setChecked(defaultOn);
    }

    private <T> void go(Class<T> cls) { startActivity(new Intent(this, cls)); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
