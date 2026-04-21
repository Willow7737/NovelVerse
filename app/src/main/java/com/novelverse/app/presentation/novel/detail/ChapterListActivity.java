package com.novelverse.app.presentation.novel.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.presentation.novel.reader.ReaderActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChapterListActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID    = "novel_id";
    public static final String EXTRA_NOVEL_TITLE = "novel_title";

    @Inject ChapterRepository chapterRepository;

    private RecyclerView rv;
    private TextView titleView, countLabel, totalLabel, sortBtn;
    private List<Chapter> allChapters = new ArrayList<>();
    private boolean newestFirst = false;
    private String novelId;
    private ChapterRowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_list);

        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        String novelTitle = getIntent().getStringExtra(EXTRA_NOVEL_TITLE);

        titleView  = findViewById(R.id.chapter_list_title);
        countLabel = findViewById(R.id.chapter_count_label);
        totalLabel = findViewById(R.id.chapters_total_label);
        sortBtn    = findViewById(R.id.btn_sort_toggle);
        rv         = findViewById(R.id.chapter_list_rv);

        if (novelTitle != null) titleView.setText(novelTitle);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new ChapterRowAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        sortBtn.setOnClickListener(v -> {
            newestFirst = !newestFirst;
            sortBtn.setText(newestFirst ? "Newest first" : "Oldest first");
            bindChapters();
        });

        if (novelId != null) loadChapters();
    }

    private void loadChapters() {
        chapterRepository.getChapters(novelId).observe(this, chapters -> {
            if (chapters == null) return;
            allChapters = chapters;
            countLabel.setText(chapters.size() + " chapters");
            totalLabel.setText(chapters.size() + " chapters total");
            bindChapters();
        });
    }

    private void bindChapters() {
        List<Chapter> display = new ArrayList<>(allChapters);
        if (newestFirst) Collections.reverse(display);
        adapter.setData(display, newestFirst ? allChapters.size() : 0, newestFirst);
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    private class ChapterRowAdapter extends RecyclerView.Adapter<ChapterRowAdapter.VH> {

        private List<Chapter> data = new ArrayList<>();
        private int totalCount = 0;
        private boolean reversed = false;

        void setData(List<Chapter> d, int total, boolean rev) {
            data = d; totalCount = total; reversed = rev;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chapter_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Chapter ch = data.get(pos);
            int realNum = reversed ? (totalCount - pos) : (pos + 1);

            h.number.setText(String.valueOf(realNum));
            String title = ch.getTitle();
            h.title.setText((title != null && !title.isEmpty()) ? title : "Chapter " + realNum);

            // Estimate read time from word count
            String content = ch.getContent();
            int words = (content != null && !content.isEmpty())
                    ? content.trim().split("\\s+").length : 0;
            int minutes = Math.max(1, words / 250);
            h.readTime.setText("~" + minutes + " min read");

            // Current chapter highlight — reuse accent
            boolean isCurrent = false; // standalone page has no "current" context
            h.number.setTextColor(isCurrent
                    ? 0xFF0085FF
                    : resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ChapterListActivity.this, ReaderActivity.class);
                i.putExtra(ReaderActivity.EXTRA_NOVEL_ID, novelId);
                i.putExtra(ReaderActivity.EXTRA_CHAPTER_ID, ch.getId());
                startActivity(i);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView number, title, readTime;
            VH(View v) {
                super(v);
                number   = v.findViewById(R.id.chapter_number);
                title    = v.findViewById(R.id.chapter_title);
                readTime = v.findViewById(R.id.chapter_read_time);
            }
        }
    }

    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
