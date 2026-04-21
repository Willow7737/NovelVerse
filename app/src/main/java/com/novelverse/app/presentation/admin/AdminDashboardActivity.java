package com.novelverse.app.presentation.admin;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.novelverse.app.ui.banner.BannerHelper;

import com.novelverse.app.presentation.base.BaseActivity;
import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminDashboardActivity extends BaseActivity {

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_admin_dashboard;
    }

    @Override
    protected void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        bindRow(R.id.row_reports,         R.drawable.ic_flag,    "Reports Queue",        "0 pending");
        bindRow(R.id.row_pending_novels,  R.drawable.ic_book_open,"Pending Approval",    "0 novels");
        bindRow(R.id.row_banned_users,    R.drawable.ic_ban,     "Banned Users",         null);
        bindRow(R.id.row_user_management, R.drawable.ic_group,   "User Management",      null);
        bindRow(R.id.row_featured_novels, R.drawable.ic_star,    "Featured Novels",      null);
        bindRow(R.id.row_promo_codes,     R.drawable.ic_tag,     "Promo Codes",          null);
        bindRow(R.id.row_app_config,      R.drawable.ic_gear,    "App Configuration",    null);
    }

    @Override
    protected void initObservers() {}

    @Override
    protected void initListeners() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requireRole("admin"); // Task 16
        super.onCreate(savedInstanceState);
    }

    private void bindRow(int rowId, int iconRes, String label, String value) {
        android.view.View row = findViewById(rowId);
        if (row == null) return;
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(iconRes);
        ((TextView)  row.findViewById(R.id.row_label)).setText(label);
        TextView valView = row.findViewById(R.id.row_value);
        if (value != null) valView.setText(value);
        else valView.setVisibility(android.view.View.GONE);
        row.setOnClickListener(v -> BannerHelper.info(this, label, "This admin panel is in active development."));
    }
}
