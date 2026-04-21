package com.novelverse.app.presentation.base;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.presentation.auth.AuthActivity;
import com.novelverse.app.ui.banner.BannerHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class BaseActivity extends AppCompatActivity {

    @Inject protected UserPreferences userPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        initViews();
        initObservers();
        initListeners();
    }

    @LayoutRes
    protected abstract int getLayoutResId();

    protected abstract void initViews();

    protected abstract void initObservers();

    protected abstract void initListeners();

    protected void showMessage(String message) {
        BannerHelper.info(this, message);
    }

    protected void showError(String message) {
        BannerHelper.error(this, message);
    }

    protected void showSuccess(String message) {
        BannerHelper.success(this, message);
    }

    protected void showWarning(String message) {
        BannerHelper.warning(this, message);
    }

    /** Task 16: Role-based navigation guard */
    protected void requireRole(String... allowedRoles) {
        String role = userPreferences != null ? userPreferences.getCachedRole() : null;
        if (role == null || role.isEmpty()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        for (String r : allowedRoles) {
            if (r.equals(role)) return;
        }
        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
        finish();
    }
}
