package com.novelverse.app.presentation.payment.subscriptions;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.novelverse.app.ui.banner.BannerHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.ProductDetails;
import com.novelverse.app.R;
import com.novelverse.app.billing.BillingManager;
import com.novelverse.app.presentation.auth.AuthViewModel;

import java.util.List;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SubscriptionActivity extends AppCompatActivity {

    @Inject BillingManager billingManager;

    private AuthViewModel authViewModel;

    private static final String[] BENEFITS = {
        "Ad-free reading experience",
        "Early access to new chapters",
        "Exclusive Premium badge on your profile",
        "Access to Premium-only novels",
        "500 monthly bonus points",
        "Priority customer support",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        buildBenefits();
        buildPlans(false);

        findViewById(R.id.tab_monthly).setOnClickListener(v -> buildPlans(false));
        findViewById(R.id.tab_annual).setOnClickListener(v  -> buildPlans(true));
    }

    private void buildPlans(boolean annual) {
        LinearLayout container = findViewById(R.id.plans_container);
        container.removeAllViews();

        String[][] plans = annual
                ? new String[][]{{"Premium", "$6.99/mo", "#6366F1", "Most Popular"}, {"VIP", "$12.99/mo", "#F59E0B", "Best Value"}}
                : new String[][]{{"Premium", "$9.99/mo", "#6366F1", ""}, {"VIP", "$18.99/mo", "#F59E0B", ""}};

        for (String[] plan : plans) {
            addPlanCard(container, plan[0], plan[1], plan[2], plan[3]);
        }
    }

    private void addPlanCard(LinearLayout container, String name, String price, String hex, String badge) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor(hex));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor(hex));
        tvName.setTextSize(18f);
        tvName.setTypeface(tvName.getTypeface(), Typeface.BOLD);
        info.addView(tvName);
        TextView tvPrice = new TextView(this);
        tvPrice.setText(price);
        tvPrice.setTextColor(resolveAttrColor(android.R.attr.textColorSecondary));
        tvPrice.setTextSize(14f);
        info.addView(tvPrice);
        if (!badge.isEmpty()) {
            TextView tvBadge = new TextView(this);
            tvBadge.setText(badge);
            tvBadge.setTextColor(Color.WHITE);
            tvBadge.setTextSize(11f);
            tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
            GradientDrawable bb = new GradientDrawable();
            bb.setColor(Color.parseColor(hex));
            bb.setCornerRadius(dp(100));
            tvBadge.setBackground(bb);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = dp(4);
            tvBadge.setLayoutParams(blp);
            info.addView(tvBadge);
        }
        card.addView(info);

        TextView btn = new TextView(this);
        btn.setText("Subscribe");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        btn.setTypeface(btn.getTypeface(), Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor(hex));
        btnBg.setCornerRadius(dp(100));
        btn.setBackground(btnBg);
        btn.setOnClickListener(v -> launchSubscription(name.toLowerCase().equals("vip")
                ? "subs_vip" : "subs_monthly"));
        card.addView(btn);
        container.addView(card);
    }

    private void launchSubscription(String productId) {
        ProductDetails details = billingManager.getProductDetails(productId);
        if (details == null) {
            BannerHelper.warning(this, "Billing not ready", "Please wait a moment and try again.");
            return;
        }
        billingManager.setBillingListener(new BillingManager.BillingListener() {
            @Override public void onProductsLoaded(List<ProductDetails> list) {}
            @Override public void onPurchaseCancelled() {}
            @Override public void onPurchaseConsumed(com.android.billingclient.api.Purchase p) {}
            @Override public void onPurchaseSuccess(com.android.billingclient.api.Purchase purchase) {
                runOnUiThread(() -> {
                    BannerHelper.success(SubscriptionActivity.this,
                            "Subscribed!", "Welcome to Premium. Enjoy your benefits!");
                    authViewModel.refreshCurrentUser(null);
                    finish();
                });
            }
            @Override public void onPurchaseError(String error) {
                runOnUiThread(() ->
                    BannerHelper.error(SubscriptionActivity.this, "Subscription failed", error));
            }
        });
        billingManager.launchPurchaseFlow(this, details);
    }

    private void buildBenefits() {
        LinearLayout container = findViewById(R.id.benefits_container);
        for (String benefit : BENEFITS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            row.setLayoutParams(lp);
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(Color.parseColor("#6366F1"));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(20), dp(20));
            ilp.rightMargin = dp(12);
            check.setLayoutParams(ilp);
            row.addView(check);
            TextView tv = new TextView(this);
            tv.setText(benefit);
            tv.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            tv.setTextSize(15f);
            row.addView(tv);
            container.addView(row);
        }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
