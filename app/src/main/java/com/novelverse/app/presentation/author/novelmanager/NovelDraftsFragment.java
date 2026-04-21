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

import java.util.ArrayList;
import java.util.List;

public class NovelDraftsFragment extends Fragment {

    private static final String ARG_NOVEL_ID = "novel_id";
    private WriterViewModel vm;
    private LinearLayout    draftsList;
    private LinearLayout    emptyView;
    private TextView        txtCount;

    public static NovelDraftsFragment newInstance(String novelId) {
        NovelDraftsFragment f = new NovelDraftsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_NOVEL_ID, novelId);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_novel_drafts, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String novelId = getArguments() != null ? getArguments().getString(ARG_NOVEL_ID) : null;
        draftsList = view.findViewById(R.id.drafts_list);
        emptyView  = view.findViewById(R.id.empty_drafts);
        txtCount   = view.findViewById(R.id.txt_draft_count);

        view.findViewById(R.id.btn_new_draft).setOnClickListener(v -> {
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
        List<Chapter> drafts = new ArrayList<>();
        if (all != null) for (Chapter c : all) if (!c.isPublished()) drafts.add(c);

        if (draftsList == null) return;
        draftsList.removeAllViews();
        if (drafts.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            draftsList.setVisibility(View.GONE);
            if (txtCount != null) txtCount.setText("0 drafts");
            return;
        }
        emptyView.setVisibility(View.GONE);
        draftsList.setVisibility(View.VISIBLE);
        if (txtCount != null) txtCount.setText(drafts.size() + " draft" + (drafts.size() == 1 ? "" : "s"));

        for (Chapter ch : drafts) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundColor(Color.WHITE);

            // Draft badge
            TextView badge = new TextView(requireContext());
            badge.setText("Draft");
            badge.setTextColor(Color.parseColor("#F59E0B"));
            badge.setTextSize(11f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(8), dp(3), dp(8), dp(3));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#FEF3C7"));
            bg.setCornerRadius(dp(100));
            badge.setBackground(bg);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bLp.rightMargin = dp(12);
            badge.setLayoutParams(bLp);
            row.addView(badge);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView title = new TextView(requireContext());
            title.setText((ch.getTitle() != null && !ch.getTitle().isEmpty())
                    ? ch.getTitle() : "Untitled Chapter " + ch.getChapterNumber());
            title.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
            title.setTextSize(14f);
            title.setTypeface(null, Typeface.BOLD);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(title);
            TextView words = new TextView(requireContext());
            words.setText(ch.getWordCount() + " words written");
            words.setTextColor(resolveAttrColor(android.R.attr.textColorTertiary));
            words.setTextSize(12f);
            info.addView(words);
            row.addView(info);

            LinearLayout wrapper = new LinearLayout(requireContext());
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row);
            View divider = new View(requireContext());
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            wrapper.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
            wrapper.setOnClickListener(v -> {
                String novelId = getArguments() != null ? getArguments().getString(ARG_NOVEL_ID) : null;
                if (novelId == null) return;
                Intent intent = new Intent(requireContext(), ChapterEditorActivity.class);
                intent.putExtra(ChapterEditorActivity.EXTRA_NOVEL_ID, novelId);
                intent.putExtra(ChapterEditorActivity.EXTRA_CHAPTER_ID, ch.getId());
                startActivity(intent);
            });
            draftsList.addView(wrapper);
        }
    }

    private int dp(int v) { return Math.round(v * requireContext().getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
