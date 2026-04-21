package com.novelverse.app.presentation.author.chaptermanager;

import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.gamification.AchievementEngine;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.presentation.write.WriterViewModel;
import com.novelverse.app.ui.banner.BannerHelper;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChapterEditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID   = "novel_id";
    public static final String EXTRA_CHAPTER_ID = "chapter_id";

    private WriterViewModel vm;
    private String novelId;
    private String chapterId;

    @Inject AchievementEngine achievementEngine;
    @Inject UserPreferences   userPreferences;

    private EditText titleInput;
    private EditText contentInput;
    private TextView wordCountView;
    private TextView readTimeView;
    private CheckBox freeChapterCheck;

    // Track which button is currently loading
    private boolean savePending    = false;
    private boolean publishPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_editor);

        vm        = new ViewModelProvider(this).get(WriterViewModel.class);
        novelId   = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        chapterId = getIntent().getStringExtra(EXTRA_CHAPTER_ID);

        titleInput       = findViewById(R.id.chapter_title_input);
        contentInput     = findViewById(R.id.chapter_content_input);
        wordCountView    = findViewById(R.id.word_count);
        readTimeView     = findViewById(R.id.read_time);
        freeChapterCheck = findViewById(R.id.chapter_free_check);

        if (freeChapterCheck != null) freeChapterCheck.setChecked(true);

        // Back
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Save draft — only dims the Save button
        View saveBtn = findViewById(R.id.btn_save_chapter);
        saveBtn.setOnClickListener(v -> {
            if (savePending) return;
            savePending = true;
            setButtonState(saveBtn, true);
            saveChapter(false);
        });

        // Publish — only dims the Publish button
        View publishBtn = findViewById(R.id.btn_publish_chapter);
        publishBtn.setOnClickListener(v -> {
            if (publishPending) return;
            publishPending = true;
            setButtonState(publishBtn, true);
            saveChapter(true);
        });

        // ── Formatting toolbar ──────────────────────────────────────────────

        View boldBtn   = findViewById(R.id.fmt_bold);
        View italicBtn = findViewById(R.id.fmt_italic);
        View noteBtn   = findViewById(R.id.fmt_note);

        if (boldBtn != null)   boldBtn.setOnClickListener(v -> applyFormat(Typeface.BOLD));
        if (italicBtn != null) italicBtn.setOnClickListener(v -> applyFormat(Typeface.ITALIC));
        if (noteBtn != null)   noteBtn.setOnClickListener(v -> insertAuthorNote());

        // Live word + read time counter
        if (contentInput != null) {
            contentInput.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    int words = s.toString().trim().isEmpty() ? 0
                            : s.toString().trim().split("\\s+").length;
                    if (wordCountView != null) wordCountView.setText(words + " words");
                    int minutes = Math.max(1, words / 200);
                    if (readTimeView != null) readTimeView.setText(minutes + " min read");
                }
            });
        }

        if (chapterId != null) loadChapter();
    }

    // ── Formatting ──────────────────────────────────────────────────────────

    /**
     * Applies BOLD or ITALIC to the currently selected text in contentInput.
     * If nothing is selected, inserts a placeholder word and formats it.
     */
    private void applyFormat(int typefaceStyle) {
        if (contentInput == null) return;
        int start = contentInput.getSelectionStart();
        int end   = contentInput.getSelectionEnd();
        if (start == end) {
            // Nothing selected — show hint
            BannerHelper.info(this, "Select text first, then tap Bold or Italic.");
            return;
        }
        Editable text = contentInput.getText();
        if (text == null) return;

        // Check if the span is already applied → toggle OFF
        StyleSpan[] existing = text.getSpans(start, end, StyleSpan.class);
        boolean alreadyApplied = false;
        for (StyleSpan s : existing) {
            if (s.getStyle() == typefaceStyle) {
                text.removeSpan(s);
                alreadyApplied = true;
            }
        }
        if (!alreadyApplied) {
            text.setSpan(new StyleSpan(typefaceStyle), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Re-set selection so user can keep editing
        contentInput.setSelection(start, end);
    }

    /** Inserts a formatted [Author's Note: …] marker at cursor position */
    private void insertAuthorNote() {
        if (contentInput == null) return;
        int cursor = contentInput.getSelectionStart();
        if (cursor < 0) cursor = contentInput.length();
        String note = "\n\n[Author's Note: ]\n\n";
        contentInput.getText().insert(cursor, note);
        // Place cursor inside the brackets
        contentInput.setSelection(cursor + note.length() - 3);
    }

    // ── Button state ─────────────────────────────────────────────────────────

    private void setButtonState(View btn, boolean loading) {
        btn.setAlpha(loading ? 0.45f : 1f);
        btn.setEnabled(!loading);
    }

    private void resetButtons() {
        savePending    = false;
        publishPending = false;
        View save    = findViewById(R.id.btn_save_chapter);
        View publish = findViewById(R.id.btn_publish_chapter);
        if (save    != null) setButtonState(save, false);
        if (publish != null) setButtonState(publish, false);
    }

    // ── Save / Publish ────────────────────────────────────────────────────────

    private void saveChapter(boolean publish) {
        String content = contentInput != null ? contentInput.getText().toString() : "";
        String title   = titleInput   != null ? titleInput.getText().toString().trim() : "";

        if (title.isEmpty()) {
            BannerHelper.warning(this, "Title required", "Give this chapter a title.");
            resetButtons();
            return;
        }
        if (content.trim().length() < 100) {
            BannerHelper.warning(this, "Too short", "Add more content before saving.");
            resetButtons();
            return;
        }

        int     wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        boolean isFree    = freeChapterCheck == null || freeChapterCheck.isChecked();

        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setWordCount(wordCount);
        chapter.setPublished(publish);
        chapter.setFree(isFree);

        NovelRepository.Callback<Chapter> cb = new NovelRepository.Callback<Chapter>() {
            @Override public void onSuccess(Chapter r) {
                runOnUiThread(() -> {
                    resetButtons();
                    if (publish) {
                        BannerHelper.success(ChapterEditorActivity.this,
                                "Chapter published!", "\"" + r.getTitle() + "\" is now live.");
                        // Fire publish achievement
                        String userId = userPreferences.getUserId();
                        if (userId != null && achievementEngine != null) {
                            int readCount   = (int) r.getTotalViews();
                            int totalPub    = userPreferences.getTotalChaptersRead(); // reuse as published proxy
                            achievementEngine.onChapterPublished(
                                ChapterEditorActivity.this, userId,
                                wordCount, readCount, totalPub,
                                System.currentTimeMillis());
                        }
                    } else {
                        BannerHelper.success(ChapterEditorActivity.this,
                                "Draft saved", "Your chapter has been saved.");
                    }
                    finish();
                });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    resetButtons();
                    BannerHelper.error(ChapterEditorActivity.this, "Save failed", e);
                });
            }
        };

        if (chapterId != null) {
            chapter.setId(chapterId);
            vm.updateChapter(chapter, cb);
        } else {
            vm.getChapters(novelId).observe(this, existingChapters -> {
                int nextNumber = 1;
                if (existingChapters != null) {
                    for (Chapter c : existingChapters) {
                        if (c.getChapterNumber() >= nextNumber) {
                            nextNumber = c.getChapterNumber() + 1;
                        }
                    }
                }
                chapter.setChapterNumber(nextNumber);
                vm.createChapter(chapter, cb);
            });
        }
    }

    private void loadChapter() {
        vm.getChapters(novelId).observe(this, chapters -> {
            if (chapters == null || chapterId == null) return;
            for (Chapter c : chapters) {
                if (chapterId.equals(c.getId())) {
                    if (titleInput   != null) titleInput.setText(c.getTitle());
                    if (contentInput != null) contentInput.setText(c.getContent());
                    break;
                }
            }
        });
    }
}
