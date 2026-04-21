package com.novelverse.app.presentation.payment.store;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.novelverse.app.ui.banner.BannerHelper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.ProductDetails;
import com.novelverse.app.R;
import com.novelverse.app.billing.BillingManager;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;

import com.google.gson.JsonObject;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PointStoreActivity extends AppCompatActivity {

    @Inject BillingManager          billingManager;
    @Inject UserPreferences         userPreferences;
    @Inject SupabaseDatabaseService dbService;

    private AuthViewModel authViewModel;

    // { points, bonus, price_cents } — product IDs must match Play Console
    private static final int[][] PACKAGES = {
        {100,  0,   99},
        {500,  50,  449},
        {1200, 200, 999},
        {3000, 600, 2399},
        {6000, 1500, 4499},
    };
    private static final String[] PRODUCT_IDS = {
        "points_100", "points_500", "points_1200", "points_3000", "points_6000"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_store);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_redeem).setOnClickListener(v -> redeemPromo());
        findViewById(R.id.btn_transaction_history).setOnClickListener(v ->
                BannerHelper.info(this, "Transaction history",
                        "Your recent purchases will appear here once billing is live."));

        // ── Daily check-in ────────────────────────────────────────────────────
        findViewById(R.id.btn_checkin).setOnClickListener(v -> {
            long lastCheckIn = userPreferences.getLastCheckInTimestamp();
            long now         = System.currentTimeMillis();
            long oneDayMs    = 24 * 60 * 60 * 1000L;
            if (now - lastCheckIn < oneDayMs) {
                BannerHelper.info(this, "Already checked in", "Come back tomorrow!");
                return;
            }
            authViewModel.addPoints(10, "daily_checkin", (success, error) ->
                runOnUiThread(() -> {
                    if (success) {
                        userPreferences.setLastCheckInTimestamp(now);
                        BannerHelper.success(this, "Daily bonus!", "+10 points earned!");
                        authViewModel.refreshCurrentUser(null);
                    } else {
                        BannerHelper.error(this, "Check-in failed", error);
                    }
                }));
        });

        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                TextView balance = findViewById(R.id.current_balance);
                if (balance != null) balance.setText(user.getPointsBalance() + " pts");
            }
        });

        buildPackages();
    }

    // ── Packages ─────────────────────────────────────────────────────────────

    private void buildPackages() {
        LinearLayout container = findViewById(R.id.packages_container);
        for (int i = 0; i < PACKAGES.length; i++) {
            final int pts    = PACKAGES[i][0];
            final int bonus  = PACKAGES[i][1];
            final int cents  = PACKAGES[i][2];
            final String pid = PRODUCT_IDS[i];

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(14));
            card.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            card.setLayoutParams(lp);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView tvPts = new TextView(this);
            tvPts.setText(pts + (bonus > 0 ? " + " + bonus + " bonus" : "") + " Points");
            tvPts.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            tvPts.setTextSize(16f);
            tvPts.setTypeface(tvPts.getTypeface(), Typeface.BOLD);
            info.addView(tvPts);
            card.addView(info);

            TextView buyBtn = new TextView(this);
            buyBtn.setText(String.format("$%.2f", cents / 100.0));
            buyBtn.setTextColor(Color.WHITE);
            buyBtn.setTextSize(15f);
            buyBtn.setTypeface(buyBtn.getTypeface(), Typeface.BOLD);
            buyBtn.setGravity(Gravity.CENTER);
            buyBtn.setPadding(dp(18), dp(10), dp(18), dp(10));
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor("#6366F1"));
            btnBg.setCornerRadius(dp(100));
            buyBtn.setBackground(btnBg);
            buyBtn.setOnClickListener(v -> launchPurchase(pid, pts + bonus));
            card.addView(buyBtn);

            container.addView(card);
        }
    }

    private void launchPurchase(String productId, int totalPoints) {
        ProductDetails details = billingManager.getProductDetails(productId);
        if (details == null) {
            BannerHelper.warning(this, "Store not ready", "Please wait a moment and try again.");
            return;
        }
        billingManager.setBillingListener(new BillingManager.BillingListener() {
            @Override public void onProductsLoaded(java.util.List<ProductDetails> list) {}
            @Override public void onPurchaseCancelled() {}
            @Override public void onPurchaseConsumed(com.android.billingclient.api.Purchase p) {}
            @Override public void onPurchaseSuccess(com.android.billingclient.api.Purchase purchase) {
                authViewModel.addPoints(totalPoints, "iap_" + productId, (success, error) ->
                    runOnUiThread(() -> {
                        if (success) {
                            BannerHelper.success(PointStoreActivity.this,
                                    "+" + totalPoints + " points added!");
                            authViewModel.refreshCurrentUser(null);
                        }
                    }));
            }
            @Override public void onPurchaseError(String error) {
                runOnUiThread(() -> BannerHelper.error(PointStoreActivity.this, "Purchase failed", error));
            }
        });
        billingManager.launchPurchaseFlow(this, details);
    }

    // ── Promo code ────────────────────────────────────────────────────────────

    private void redeemPromo() {
        EditText input = findViewById(R.id.promo_input);
        String code = input != null ? input.getText().toString().trim() : "";
        if (code.isEmpty()) {
            BannerHelper.warning(this, "Enter a promo code first");
            return;
        }
        BannerHelper.info(this, "Checking code…");

        String userId = userPreferences.getUserId();
        String token  = userPreferences.getAccessToken();
        if (userId == null || token == null) return;

        JsonObject params = new JsonObject();
        params.addProperty("p_user_id", userId);
        params.addProperty("p_code",    code);

        dbService.callRpc("redeem_promo_code", params, token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        try {
                            JsonObject obj = new com.google.gson.Gson().fromJson(result, JsonObject.class);
                            boolean valid  = obj.has("valid") && obj.get("valid").getAsBoolean();
                            String message = obj.has("message") ? obj.get("message").getAsString() : "";
                            int points     = obj.has("points") ? obj.get("points").getAsInt() : 0;
                            if (valid && points > 0) {
                                authViewModel.addPoints(points, "promo_" + code, (s, e) ->
                                    runOnUiThread(() -> {
                                        BannerHelper.success(PointStoreActivity.this,
                                            "Code redeemed!", "+" + points + " points added!");
                                        authViewModel.refreshCurrentUser(null);
                                        if (input != null) input.setText("");
                                    }));
                            } else {
                                BannerHelper.error(PointStoreActivity.this,
                                        message.isEmpty() ? "Invalid code" : message);
                            }
                        } catch (Exception e) {
                            BannerHelper.error(PointStoreActivity.this, "Could not redeem code");
                        }
                    });
                }
                @Override public void onError(String error) {
                    runOnUiThread(() ->
                        BannerHelper.error(PointStoreActivity.this, "Error checking code", error));
                }
            });
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
