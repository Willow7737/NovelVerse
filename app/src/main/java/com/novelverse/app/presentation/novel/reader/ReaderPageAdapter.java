package com.novelverse.app.presentation.novel.reader;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;

import java.util.ArrayList;
import java.util.List;

/** Splits chapter content into pages for paged reading mode, with full preference support. */
public class ReaderPageAdapter extends RecyclerView.Adapter<ReaderPageAdapter.PageVH> {

    private static final int PAGE_SIZE = 1500;
    private final List<String> pages = new ArrayList<>();

    // Styling — applied in onBindViewHolder
    private int   fontSizeSp      = 17;
    private float lineSpacing     = 1.6f;
    private float letterSpacing   = 0f;
    private int   bgColor         = 0xFFFFFFFF;
    private int   textColor       = 0xFF1A1A1A;
    private int   sidePaddingPx   = 40;
    private int   fontResId       = R.font.inter_regular;

    public void setContent(String content) {
        pages.clear();
        if (content == null || content.isEmpty()) { notifyDataSetChanged(); return; }
        int len = content.length(), start = 0;
        while (start < len) {
            int end = Math.min(start + PAGE_SIZE, len);
            if (end < len) { int sp = content.lastIndexOf(' ', end); if (sp > start) end = sp; }
            pages.add(content.substring(start, end).trim());
            start = end + 1;
        }
        notifyDataSetChanged();
    }

    public void applyPreferences(int fontSizeSp, float lineSpacing, float letterSpacing,
                                  int bgColor, int textColor, int sidePaddingPx, Context context) {
        this.fontSizeSp    = fontSizeSp;
        this.lineSpacing   = lineSpacing;
        this.letterSpacing = letterSpacing;
        this.bgColor       = bgColor;
        this.textColor     = textColor;
        this.sidePaddingPx = sidePaddingPx;
        notifyDataSetChanged();
    }

    public void setFontRes(int resId) { this.fontResId = resId; notifyDataSetChanged(); }

    @NonNull @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reader_page, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH h, int pos) {
        h.content.setText(pages.get(pos));
        h.content.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        h.content.setLineSpacing(0f, lineSpacing);
        h.content.setLetterSpacing(letterSpacing);
        h.content.setTextColor(textColor);
        h.content.setBackgroundColor(bgColor);
        h.itemView.setBackgroundColor(bgColor);
        h.content.setPadding(sidePaddingPx, h.content.getPaddingTop(),
                             sidePaddingPx, h.content.getPaddingBottom());
        try {
            Typeface tf = ResourcesCompat.getFont(h.content.getContext(), fontResId);
            if (tf != null) h.content.setTypeface(tf);
        } catch (Exception ignored) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            h.content.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            h.content.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }
    }

    @Override public int getItemCount() { return pages.size(); }

    static class PageVH extends RecyclerView.ViewHolder {
        TextView content;
        PageVH(View v) { super(v); content = v.findViewById(R.id.page_content); }
    }
}
