package com.novelverse.app.presentation.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.ui.banner.BannerHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Notifications screen — loads the user's recent notifications from Supabase
 * with pull-to-refresh and mark-all-read support.
 */
@AndroidEntryPoint
public class NotificationsFragment extends Fragment {

    @Inject SupabaseDatabaseService dbService;
    @Inject UserPreferences         userPreferences;

    private SwipeRefreshLayout  swipeRefresh;
    private RecyclerView        recycler;
    private NotificationAdapter adapter;
    private TextView            btnMarkAllRead;

    private final List<NotificationItem> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefresh   = view.findViewById(R.id.swipe_refresh);
        recycler       = view.findViewById(R.id.notifications_recycler);
        btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);

        adapter = new NotificationAdapter(items, this::onNotificationTapped);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recycler.setAdapter(adapter);

        if (swipeRefresh != null) swipeRefresh.setOnRefreshListener(this::loadNotifications);
        if (btnMarkAllRead != null) btnMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadNotifications() {
        String token  = userPreferences.getAccessToken();
        String userId = userPreferences.getUserId();
        if (userId == null || userId.isEmpty()) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        String url = dbService.buildSelectUrl(
                "notifications",
                "id,type,title,body,is_read,created_at",
                "user_id=eq." + userId + "&order=created_at.desc&limit=50",
                null);

        dbService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    parseAndRender(result);
                });
            }
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    BannerHelper.error(requireActivity(), "Couldn't load notifications");
                });
            }
        });
    }

    private void parseAndRender(String json) {
        items.clear();
        try {
            JsonArray arr = new Gson().fromJson(json, JsonArray.class);
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject o = arr.get(i).getAsJsonObject();
                    items.add(new NotificationItem(
                            o.has("id")         ? o.get("id").getAsString()         : "",
                            o.has("type")       ? o.get("type").getAsString()       : "system",
                            o.has("title")      ? o.get("title").getAsString()      : "",
                            o.has("body")       ? o.get("body").getAsString()       : "",
                            o.has("is_read") && o.get("is_read").getAsBoolean(),
                            o.has("created_at") ? o.get("created_at").getAsString() : ""
                    ));
                }
            }
        } catch (Exception ignored) {}
        adapter.notifyDataSetChanged();
    }

    private void onNotificationTapped(NotificationItem item) {
        if (!item.isRead()) {
            markRead(item.getId());
            item.setRead(true);
            adapter.notifyDataSetChanged();
        }
        // Deep-link based on notification type handled here in future phases
    }

    private void markRead(String id) {
        String token = userPreferences.getAccessToken();
        JsonObject patch = new JsonObject();
        patch.addProperty("is_read", true);
        dbService.update("notifications", id, patch, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {}
                    @Override public void onError(String e) {}
                });
    }

    private void markAllRead() {
        String userId = userPreferences.getUserId();
        if (userId == null) return;
        for (NotificationItem i : items) i.setRead(true);
        adapter.notifyDataSetChanged();
        // Server patch via RPC / batch update can be added here
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    public static class NotificationItem {
        private final String id, type, title, body, createdAt;
        private boolean read;

        public NotificationItem(String id, String type, String title,
                                String body, boolean read, String createdAt) {
            this.id = id; this.type = type; this.title = title;
            this.body = body; this.read = read; this.createdAt = createdAt;
        }

        public String  getId()        { return id; }
        public String  getType()      { return type; }
        public String  getTitle()     { return title; }
        public String  getBody()      { return body; }
        public boolean isRead()       { return read; }
        public void    setRead(boolean r) { this.read = r; }
        public String  getCreatedAt() { return createdAt; }
    }
}
