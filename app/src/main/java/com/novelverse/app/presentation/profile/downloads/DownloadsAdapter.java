package com.novelverse.app.presentation.profile.downloads;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.NovelEntity;

/**
 * RecyclerView adapter for the offline Downloads list. Uses {@link ListAdapter} with {@link
 * DiffUtil} for efficient updates.
 */
public class DownloadsAdapter extends ListAdapter<NovelEntity, DownloadsAdapter.VH> {

    public interface OnItemClick {
        void onClick(NovelEntity novel);
    }

    private final OnItemClick listener;

    public DownloadsAdapter(OnItemClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_novel_library, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView author;
        private final TextView chapters;

        VH(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.novel_cover);
            title = itemView.findViewById(R.id.novel_title);
            author = itemView.findViewById(R.id.novel_author);
            chapters = itemView.findViewById(R.id.novel_chapters);
        }

        void bind(NovelEntity novel, OnItemClick listener) {
            if (title != null) title.setText(novel.getTitle());
            if (author != null) author.setText(novel.getAuthorDisplayName()); // <-- changed
            if (chapters != null) {
                Integer ch = novel.getTotalChapters(); // <-- changed
                chapters.setText((ch != null ? ch : 0) + " chapters • Offline");
            }
            if (cover != null && novel.getCoverImageUrl() != null) {
                Glide.with(cover.getContext())
                        .load(novel.getCoverImageUrl())
                        .placeholder(R.drawable.img_placeholder_cover)
                        .into(cover);
            }
            itemView.setOnClickListener(
                    v -> {
                        if (listener != null) listener.onClick(novel);
                    });
        }
    }

    private static final DiffUtil.ItemCallback<NovelEntity> DIFF =
            new DiffUtil.ItemCallback<NovelEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull NovelEntity a, @NonNull NovelEntity b) {
                    return a.getId().equals(b.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NovelEntity a, @NonNull NovelEntity b) {
                    return a.getId().equals(b.getId()) && a.getTitle().equals(b.getTitle());
                }
            };
}
