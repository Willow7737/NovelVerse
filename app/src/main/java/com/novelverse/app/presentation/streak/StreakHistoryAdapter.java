package com.novelverse.app.presentation.streak;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.StreakHistoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for the Past Streaks list in StreakActivity.
 *
 * Each row shows:
 *   🔥 {N} day streak  |  {date range}  |  {time ago}
 */
public class StreakHistoryAdapter
        extends RecyclerView.Adapter<StreakHistoryAdapter.ViewHolder> {

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d", Locale.getDefault());

    private List<StreakHistoryItem> items = new ArrayList<>();

    public void submitList(List<StreakHistoryItem> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_streak_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvLength;
        private final TextView tvDates;
        private final TextView tvTimeAgo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLength  = itemView.findViewById(R.id.tv_history_length);
            tvDates   = itemView.findViewById(R.id.tv_history_dates);
            tvTimeAgo = itemView.findViewById(R.id.tv_history_time_ago);
        }

        void bind(StreakHistoryItem item) {
            // Streak length label
            int len = item.streakLength;
            tvLength.setText(len + (len == 1 ? " day streak" : " day streak"));

            // Date range
            if (item.startedAtMs > 0 && item.endedAtMs > 0) {
                String start = DATE_FMT.format(new Date(item.startedAtMs));
                String end   = DATE_FMT.format(new Date(item.endedAtMs));
                tvDates.setText(start + " – " + end);
            } else if (item.endedAtMs > 0) {
                tvDates.setText("Ended " + DATE_FMT.format(new Date(item.endedAtMs)));
            } else {
                tvDates.setText("");
            }

            // Time ago
            if (item.endedAtMs > 0) {
                long nowMs = System.currentTimeMillis();
                tvTimeAgo.setText(formatTimeAgo(nowMs - item.endedAtMs));
            } else {
                tvTimeAgo.setText("");
            }
        }

        private String formatTimeAgo(long diffMs) {
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            if (hours < 24)   return hours + "h ago";
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            if (days < 30)    return days + "d ago";
            long weeks = days / 7;
            if (weeks < 8)    return weeks + "w ago";
            return (days / 30) + "mo ago";
        }
    }
}
