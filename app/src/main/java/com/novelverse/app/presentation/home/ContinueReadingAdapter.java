package com.novelverse.app.presentation.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;

public class ContinueReadingAdapter extends ListAdapter<Novel, ContinueReadingAdapter.VH> {

    public interface OnItemClickListener { void onItemClick(Novel novel); }
    private OnItemClickListener listener;

    public ContinueReadingAdapter() {
        super(new DiffUtil.ItemCallback<Novel>() {
            @Override public boolean areItemsTheSame(@NonNull Novel a, @NonNull Novel b) {
                return a.getId() != null && a.getId().equals(b.getId());
            }
            @Override public boolean areContentsTheSame(@NonNull Novel a, @NonNull Novel b) {
                return a.getId() != null && a.getId().equals(b.getId())
                    && a.getReadingProgressPercent() == b.getReadingProgressPercent()
                    && a.getCurrentChapterNumber() == b.getCurrentChapterNumber();
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_continue_reading, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Novel novel = getItem(pos);

        h.title.setText(novel.getTitle() != null ? novel.getTitle() : "");
        h.author.setText(novel.getAuthorDisplayName() != null ? novel.getAuthorDisplayName() : "");

        // Chapter label
        int chNum = novel.getCurrentChapterNumber();
        if (chNum > 0) {
            h.chapterLabel.setText("Ch. " + chNum);
        } else {
            h.chapterLabel.setText("Ch. 1");
        }

        // Progress bar + text
        double pct = novel.getReadingProgressPercent();
        int progress = (int) Math.min(100, Math.max(0, pct));
        h.progressBar.setProgress(progress);
        h.progressText.setText(progress + "%");

        // Cover image
        if (novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty()) {
            Glide.with(h.cover.getContext())
                    .load(novel.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover)
                    .centerCrop()
                    .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.img_placeholder_cover);
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(novel); });
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView   cover;
        TextView    title, author, chapterLabel, progressText;
        ProgressBar progressBar;

        VH(View v) {
            super(v);
            cover         = v.findViewById(R.id.cover_image);
            title         = v.findViewById(R.id.title_text);
            author        = v.findViewById(R.id.author_text);
            chapterLabel  = v.findViewById(R.id.chapter_label);
            progressText  = v.findViewById(R.id.progress_text);
            progressBar   = v.findViewById(R.id.reading_progress_bar);
        }
    }
}
