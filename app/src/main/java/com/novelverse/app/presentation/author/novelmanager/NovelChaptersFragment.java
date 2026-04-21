package com.novelverse.app.presentation.author.novelmanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.presentation.author.chaptermanager.ChapterEditorActivity;
import com.novelverse.app.presentation.write.WriterViewModel;

import java.util.List;

public class NovelChaptersFragment extends Fragment {

    private static final String ARG_NOVEL_ID = "novel_id";
    private WriterViewModel vm;
    private LinearLayout    chaptersList;
    private LinearLayout    emptyView;
    private TextView        txtCount;

    public static NovelChaptersFragment newInstance(String novelId) {
        NovelChaptersFragment f = new NovelChaptersFragment();
        Bundle b = new Bundle();
        b.putString(ARG_NOVEL_ID, novelId);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_novel_chapters, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String novelId = getArguments() != null ? getArguments().getString(ARG_NOVEL_ID) : null;
        chaptersList = view.findViewById(R.id.chapters_list);
        emptyView    = view.findViewById(R.id.empty_chapters);
        txtCount     = view.findViewById(R.id.txt_chapter_count);

        view.findViewById(R.id.btn_add_chapter).setOnClickListener(v -> {
            if (novelId == null) return;
            Intent intent = new Intent(requireContext(), ChapterEditorActivity.class);
            intent.putExtra(ChapterEditorActivity.EXTRA_NOVEL_ID, novelId);
            startActivity(intent);
        });

        if (novelId == null) return;
        vm = new ViewModelProvider(requireActivity()).get(WriterViewModel.class);
        vm.getChapters(novelId).observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Chapter> all) {
        if (chaptersList == null) return;
        // Show only published chapters in this tab
        List<Chapter> published = new java.util.ArrayList<>();
        if (all != null) for (Chapter c : all) if (c.isPublished()) published.add(c);

        chaptersList.removeAllViews();
        if (published.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            chaptersList.setVisibility(View.GONE);
            if (txtCount != null) txtCount.setText("0 chapters");
            return;
        }
        emptyView.setVisibility(View.GONE);
        chaptersList.setVisibility(View.VISIBLE);
        if (txtCount != null) txtCount.setText(published.size() + " chapter" + (published.size() == 1 ? "" : "s"));
        for (Chapter ch : published) chaptersList.addView(buildRow(ch));
    }

    private View buildRow(Chapter ch) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        // Use theme surface color — adapts to dark mode automatically
        row.setBackgroundColor(resolveAttrColor(com.google.android.material.R.attr.colorSurface));

        // Chapter number badge
        TextView num = new TextView(requireContext());
        num.setText(String.valueOf(ch.getChapterNumber()));
        num.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorPrimary));
        num.setTextSize(13f);
        num.setTypeface(null, Typeface.BOLD);
        num.setGravity(android.view.Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        // Use theme surfaceVariant so badge is visible in both light and dark mode
        bg.setColor(resolveAttrColor(com.google.android.material.R.attr.colorSurfaceVariant));
        bg.setCornerRadius(dp(100));
        num.setBackground(bg);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        nLp.rightMargin = dp(12);
        num.setLayoutParams(nLp);
        row.addView(num);

        // Title + word count
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = new TextView(requireContext());
        title.setText(ch.getTitle() != null ? ch.getTitle() : "Chapter " + ch.getChapterNumber());
        title.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurface));
        title.setTextSize(14f);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(title);
        TextView meta = new TextView(requireContext());
        meta.setText(ch.getWordCount() + " words  ·  " + ch.getTotalViews() + " views");
        meta.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        meta.setTextSize(12f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.topMargin = dp(2);
        meta.setLayoutParams(mLp);
        info.addView(meta);
        row.addView(info);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dLp.topMargin = dp(0);

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        View div = new View(requireContext());
        // Use themed divider color — light mode gets #E3E8ED, dark mode gets #253344
        div.setBackgroundColor(requireContext().getResources().getColor(R.color.divider, requireContext().getTheme()));
        wrapper.addView(div, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        wrapper.setOnClickListener(v -> {
            String novelId = getArguments() != null ? getArguments().getString(ARG_NOVEL_ID) : null;
            if (novelId == null) return;
            Intent intent = new Intent(requireContext(), ChapterEditorActivity.class);
            intent.putExtra(ChapterEditorActivity.EXTRA_NOVEL_ID, novelId);
            intent.putExtra(ChapterEditorActivity.EXTRA_CHAPTER_ID, ch.getId());
            startActivity(intent);
        });
        return wrapper;
    }

    private int dp(int v) { return Math.round(v * requireContext().getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
