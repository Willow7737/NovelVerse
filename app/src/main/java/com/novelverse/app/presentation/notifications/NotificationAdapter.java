package com.novelverse.app.presentation.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.presentation.notifications.NotificationsFragment.NotificationItem;

import java.util.List;

/**
 * RecyclerView adapter for the notifications list.
 *
 * Unread items are rendered bold; read items are styled with reduced alpha
 * to give a clear visual distinction without requiring a separate layout.
 */
public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClick {
        void onClick(NotificationItem item);
    }

    private final List<NotificationItem> items;
    private final OnItemClick            listener;

    public NotificationAdapter(List<NotificationItem> items, OnItemClick listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvTime;
        private final View     dotUnread;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle  = itemView.findViewById(R.id.notification_title);
            tvBody   = itemView.findViewById(R.id.notification_body);
            tvTime   = itemView.findViewById(R.id.notification_time);
            dotUnread = itemView.findViewById(R.id.notification_unread_dot);
        }

        void bind(NotificationItem item, OnItemClick listener) {
            if (tvTitle  != null) tvTitle.setText(item.getTitle());
            if (tvBody   != null) tvBody.setText(item.getBody());
            if (tvTime   != null) tvTime.setText(formatRelativeTime(item.getCreatedAt()));
            if (dotUnread != null) dotUnread.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Bold title for unread notifications
            if (tvTitle != null) {
                tvTitle.setTypeface(null, item.isRead() ? Typeface.NORMAL : Typeface.BOLD);
            }
            itemView.setAlpha(item.isRead() ? 0.7f : 1.0f);
            itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(item); });
        }

        /** Returns a human-readable relative time string from an ISO-8601 timestamp. */
        private String formatRelativeTime(String iso) {
            if (iso == null || iso.isEmpty()) return "";
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = sdf.parse(iso.length() > 19 ? iso.substring(0, 19) : iso);
                if (date == null) return "";
                long diff = System.currentTimeMillis() - date.getTime();
                long minutes = diff / 60_000;
                if (minutes < 1)   return "Just now";
                if (minutes < 60)  return minutes + "m ago";
                long hours = minutes / 60;
                if (hours < 24)    return hours + "h ago";
                long days = hours / 24;
                if (days < 7)      return days + "d ago";
                return new java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(date);
            } catch (Exception e) {
                return "";
            }
        }
    }
}
