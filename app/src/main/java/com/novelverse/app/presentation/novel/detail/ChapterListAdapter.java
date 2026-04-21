package com.novelverse.app.presentation.novel.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.domain.models.Chapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Task 43: Chapter list adapter.
 * Shows first 10 by default; "View All X Chapters" footer expands the list.
 */
public class ChapterListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHAPTER = 0;
    private static final int TYPE_FOOTER  = 1;
    private static final int DEFAULT_SHOW = 10;

    public interface OnChapterClickListener { void onClick(String chapterId); }

    private final List<Chapter> allChapters;
    private final OnChapterClickListener listener;
    private boolean expanded = false;
    private boolean reversed = false;

    public ChapterListAdapter(List<Chapter> chapters, OnChapterClickListener listener) {
        this.allChapters = chapters != null ? chapters : new ArrayList<>();
        this.listener    = listener;
    }

    public void setReversed(boolean rev) {
        this.reversed = rev;
        notifyDataSetChanged();
    }

    private List<Chapter> visibleChapters() {
        List<Chapter> source = reversed
                ? new java.util.ArrayList<>(allChapters)
                : allChapters;
        if (reversed) java.util.Collections.reverse((java.util.ArrayList<Chapter>) source);
        if (expanded || source.size() <= DEFAULT_SHOW) return source;
        return source.subList(0, DEFAULT_SHOW);
    }

    private boolean hasFooter() { return !expanded && allChapters.size() > DEFAULT_SHOW; }

    @Override public int getItemCount() { return visibleChapters().size() + (hasFooter() ? 1 : 0); }
    @Override public int getItemViewType(int pos) {
        return (hasFooter() && pos == visibleChapters().size()) ? TYPE_FOOTER : TYPE_CHAPTER;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            View v = inf.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new FooterVH(v);
        }
        // Chapter row — simple programmatic layout
        android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(parent, 16), dp(parent, 14), dp(parent, 16), dp(parent, 14));
        row.setBackground(parent.getContext().getDrawable(android.R.color.transparent));

        TextView num = new TextView(parent.getContext());
        num.setId(android.R.id.text1);
        num.setTextSize(13f);
        // Use theme-aware secondary text color so it adapts to dark mode
        num.setTextColor(resolveColorAttr(parent.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
        num.setMinWidth(dp(parent, 40));
        row.addView(num);

        TextView title = new TextView(parent.getContext());
        title.setId(android.R.id.text2);
        title.setTextSize(15f);
        // Resolve colorOnSurface from the current theme so dark mode gets light text
        title.setTextColor(resolveColorAttr(parent.getContext(), com.google.android.material.R.attr.colorOnSurface));
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(lp);
        row.addView(title);

        return new ChapterVH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        if (getItemViewType(pos) == TYPE_FOOTER) {
            int remaining = allChapters.size() - DEFAULT_SHOW;
            ((FooterVH) holder).text.setText("View All " + allChapters.size() + " Chapters ▾");
            holder.itemView.setOnClickListener(v -> { expanded = true; notifyDataSetChanged(); });
            return;
        }
        Chapter ch = visibleChapters().get(pos);
        ChapterVH h = (ChapterVH) holder;
        h.number.setText("Ch." + (pos + 1));
        h.title.setText(ch.getTitle() != null ? ch.getTitle() : "Chapter " + (pos + 1));
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(ch.getId()); });
    }

    static class ChapterVH extends RecyclerView.ViewHolder {
        TextView number, title;
        ChapterVH(View v) {
            super(v);
            number = v.findViewById(android.R.id.text1);
            title  = v.findViewById(android.R.id.text2);
        }
    }

    static class FooterVH extends RecyclerView.ViewHolder {
        TextView text;
        FooterVH(View v) {
            super(v);
            text = v.findViewById(android.R.id.text1);
            text.setTextSize(14f);
            text.setTextColor(0xFF6366F1);
            text.setPadding(48, 20, 48, 20);
            text.setGravity(android.view.Gravity.CENTER);
        }
    }

    private static int dp(ViewGroup parent, int dp) {
        return Math.round(dp * parent.getContext().getResources().getDisplayMetrics().density);
    }

    /** Resolves a theme color attribute (e.g. colorOnSurface) from the view's current theme. */
    private static int resolveColorAttr(android.content.Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
