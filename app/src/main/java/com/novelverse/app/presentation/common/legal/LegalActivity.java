package com.novelverse.app.presentation.common.legal;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Displays Privacy Policy or Terms of Service.
 * Pass EXTRA_DOC = "privacy" | "terms" to open a specific tab directly.
 */
@AndroidEntryPoint
public class LegalActivity extends AppCompatActivity {

    public static final String EXTRA_DOC = "doc";

    private static final String PRIVACY_TEXT =
        "Privacy Policy\n\nLast updated: January 2025\n\n" +
        "NovelVerse (\"we\", \"us\", or \"our\") is committed to protecting your privacy. " +
        "This policy explains what information we collect, how we use it, and your rights.\n\n" +
        "Information We Collect\n\n" +
        "We collect information you provide directly to us, such as when you create an account, " +
        "write or read novels, make purchases, or contact us for support.\n\n" +
        "How We Use Your Information\n\n" +
        "We use your information to provide, improve, and personalise our services, " +
        "process transactions, send notifications you've requested, and ensure platform safety.\n\n" +
        "Data Retention\n\n" +
        "We retain your data for as long as your account is active or as needed to provide services. " +
        "You may request deletion of your account and associated data at any time.\n\n" +
        "Contact\n\nFor privacy questions: privacy@novelverse.app";

    private static final String TERMS_TEXT =
        "Terms of Service\n\nLast updated: January 2025\n\n" +
        "By using NovelVerse you agree to these terms. Please read them carefully.\n\n" +
        "1. Accounts\n\n" +
        "You must be 13 or older to use NovelVerse. You are responsible for maintaining " +
        "the security of your account credentials.\n\n" +
        "2. Content\n\n" +
        "You retain ownership of content you create on NovelVerse. By publishing, you grant " +
        "NovelVerse a non-exclusive licence to display and distribute your content on the platform.\n\n" +
        "3. Prohibited Conduct\n\n" +
        "You may not upload content that is illegal, harmful, or violates others' rights. " +
        "Violations may result in content removal or account termination.\n\n" +
        "4. Payments\n\n" +
        "All purchases are final unless required by law. Point balances have no cash value.\n\n" +
        "5. Contact\n\nlegal@novelverse.app";

    private boolean showingPrivacy = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        String doc = getIntent().getStringExtra(EXTRA_DOC);
        if ("terms".equals(doc)) showingPrivacy = false;

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.tab_privacy).setOnClickListener(v -> switchDoc(true));
        findViewById(R.id.tab_terms).setOnClickListener(v -> switchDoc(false));

        switchDoc(showingPrivacy);
    }

    private void switchDoc(boolean privacy) {
        showingPrivacy = privacy;
        TextView content = findViewById(R.id.legal_content);
        content.setText(privacy ? PRIVACY_TEXT : TERMS_TEXT);

        styleTab(R.id.tab_privacy, privacy);
        styleTab(R.id.tab_terms, !privacy);
    }

    private void styleTab(int id, boolean active) {
        TextView tab = findViewById(id);
        if (active) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#6366F1"));
            bg.setCornerRadius(100);
            tab.setBackground(bg);
            tab.setTextColor(Color.WHITE);
        } else {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#F1F5F9"));
            bg.setCornerRadius(100);
            tab.setBackground(bg);
            tab.setTextColor(resolveAttrColor(android.R.attr.textColorSecondary));
        }
    }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
