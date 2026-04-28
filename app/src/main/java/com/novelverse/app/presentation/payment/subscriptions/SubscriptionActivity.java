package com.novelverse.app.presentation.payment.subscriptions;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.novelverse.app.R;
import com.novelverse.app.billing.BillingManager;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SubscriptionActivity extends AppCompatActivity {

    @Inject BillingManager billingManager;

    private AuthViewModel authViewModel;

    private TextView tabMonthly;
    private TextView tabAnnual;
    private View indicatorView;
    private LinearLayout plansContainer;

    private boolean isAnnual = false;

    private static final String[] BENEFITS = {
        "Ad-free reading experience",
        "Early access to new chapters",
        "Exclusive Premium badge on your profile",
        "Access to Premium-only novels",
        "500 monthly bonus points",
        "Priority customer support",
    };

    private final List<PlanData> monthlyPlans = Arrays.asList(
        new PlanData("Premium", "$9.99/mo", "#6366F1", ""),
        new PlanData("VIP", "$18.99/mo", "#F59E0B", "")
    );

    private final List<PlanData> annualPlans = Arrays.asList(
        new PlanData("Premium", "$6.99/mo", "#6366F1", "Most Popular"),
        new PlanData("VIP", "$12.99/mo", "#F59E0B", "Best Value")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        setupTabSwitcher();
        buildBenefits();
        showPlans(false);
    }

    private void initViews() {
        tabMonthly = findViewById(R.id.tab_monthly);
        tabAnnual = findViewById(R.id.tab_annual);
        indicatorView = findViewById(R.id.tab_indicator);
        plansContainer = findViewById(R.id.plans_container);
    }

    private void setupTabSwitcher() {
        tabMonthly.setOnClickListener(v -> {
            if (!isAnnual) return;
            isAnnual = false;
            animateTabSwitch(tabMonthly, tabAnnual);
            animatePlansChange(false);
        });

        tabAnnual.setOnClickListener(v -> {
            if (isAnnual) return;
            isAnnual = true;
            animateTabSwitch(tabAnnual, tabMonthly);
            animatePlansChange(true);
        });
    }

    private void animateTabSwitch(TextView selected, TextView unselected) {
        float targetX = selected.getX();
        ObjectAnimator indicatorAnim = ObjectAnimator.ofFloat(indicatorView, "x", targetX);
        indicatorAnim.setDuration(300);
        indicatorAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        indicatorAnim.start();

        selected.setTextColor(Color.WHITE);
        selected.setTypeface(null, Typeface.BOLD);
        unselected.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        unselected.setTypeface(null, Typeface.NORMAL);

        selected.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .start();

        unselected.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start();
    }

    private void animatePlansChange(boolean annual) {
        plansContainer.animate()
            .alpha(0f)
            .translationY(-30)
            .setDuration(200)
            .withEndAction(() -> {
                showPlans(annual);
                plansContainer.setAlpha(0f);
                plansContainer.setTranslationY(30);
                plansContainer.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            })
            .start();
    }

    private void showPlans(boolean annual) {
        plansContainer.removeAllViews();
        List<PlanData> plans = annual ? annualPlans : monthlyPlans;

        for (int i = 0; i < plans.size(); i++) {
            PlanData plan = plans.get(i);
            View planView = getLayoutInflater().inflate(R.layout.item_subscription_plan, plansContainer, false);

            TextView name = planView.findViewById(R.id.tv_plan_name);
            TextView price = planView.findViewById(R.id.tv_plan_price);
            TextView badge = planView.findViewById(R.id.tv_badge);
            View card = planView.findViewById(R.id.plan_card);
            TextView btnSubscribe = planView.findViewById(R.id.btn_subscribe);

            name.setText(plan.name);
            price.setText(plan.price);
            badge.setText(plan.badge);
            badge.setVisibility(plan.badge.isEmpty() ? View.GONE : View.VISIBLE);

            int color = Color.parseColor(plan.colorHex);
            ((GradientDrawable) card.getBackground()).setStroke(dp(2), color);
            name.setTextColor(color);
            ((GradientDrawable) btnSubscribe.getBackground()).setColor(color);
            ((GradientDrawable) badge.getBackground()).setColor(color);

            btnSubscribe.setOnClickListener(v -> launchSubscription(plan.name.toLowerCase()));

            planView.setAlpha(0f);
            planView.setTranslationY(50);
            plansContainer.addView(planView);

            planView.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(400)
                .setStartDelay(i * 100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    private void launchSubscription(String planName) {
        String productId = planName.equals("vip") ? "subs_vip" : "subs_monthly";
        ProductDetails details = billingManager.getProductDetails(productId);
        if (details == null) {
            BannerHelper.warning(this, "Billing not ready", "Please wait a moment and try again.");
            return;
        }
        billingManager.setBillingListener(new BillingManager.BillingListener() {
            @Override public void onProductsLoaded(List<ProductDetails> list) {}
            @Override public void onPurchaseCancelled() {}
            @Override public void onPurchaseConsumed(Purchase p) {}
            @Override public void onPurchaseSuccess(Purchase purchase) {
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
        container.removeAllViews();
        for (String benefit : BENEFITS) {
            View benefitView = getLayoutInflater().inflate(R.layout.item_benefit, container, false);
            TextView tv = benefitView.findViewById(R.id.tv_benefit);
            tv.setText(benefit);
            container.addView(benefitView);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class PlanData {
        String name;
        String price;
        String colorHex;
        String badge;

        PlanData(String name, String price, String colorHex, String badge) {
            this.name = name;
            this.price = price;
            this.colorHex = colorHex;
            this.badge = badge;
        }
    }
}
