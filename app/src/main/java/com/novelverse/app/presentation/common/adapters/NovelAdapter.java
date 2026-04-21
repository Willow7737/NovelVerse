package com.novelverse.app.presentation.common.adapters;

import android.view.LayoutInflater;
import android.app.Activity;
import android.view.View;
import androidx.core.app.ActivityOptionsCompat;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.Novel;

/**
 * Adapter for displaying novels in RecyclerView
 */
public class NovelAdapter extends ListAdapter<Novel, NovelAdapter.NovelViewHolder> {

    public static final int VIEW_TYPE_HORIZONTAL = 1;
    public static final int VIEW_TYPE_VERTICAL = 2;
    public static final int VIEW_TYPE_GRID = 3;
    public static final int VIEW_TYPE_LIBRARY = 4;

    private final int viewType;
    private OnItemClickListener listener;

    public NovelAdapter(int viewType) {
        super(new NovelDiffCallback());
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NovelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        switch (this.viewType) {
            case VIEW_TYPE_HORIZONTAL:
                layoutRes = R.layout.item_novel_horizontal;
                break;
            case VIEW_TYPE_GRID:
                layoutRes = R.layout.item_novel_grid;
                break;
            case VIEW_TYPE_LIBRARY:
                layoutRes = R.layout.item_novel_library;
                break;
            case VIEW_TYPE_VERTICAL:
            default:
                layoutRes = R.layout.item_novel_vertical;
                break;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new NovelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelViewHolder holder, int position) {
        Novel novel = getItem(position);
        holder.bind(novel);
    }

    public class NovelViewHolder extends RecyclerView.ViewHolder {

        private final ImageView coverImage;
        private final TextView titleText;
        private final TextView authorText;
        private final TextView chapterCountText;
        private final RatingBar ratingBar;
        private final TextView ratingText;

        public NovelViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.cover_image);
            titleText = itemView.findViewById(R.id.title_text);
            authorText = itemView.findViewById(R.id.author_text);
            chapterCountText = itemView.findViewById(R.id.chapter_count_text);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            ratingText = itemView.findViewById(R.id.rating_text);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(position));
                }
            });
        }

        public void bind(Novel novel) {
            titleText.setText(novel.getTitle());
            authorText.setText(novel.getAuthorDisplayName());
            if (chapterCountText != null) {
                chapterCountText.setText(novel.getFormattedChapterCount());
            }

            if (ratingBar != null) {
                ratingBar.setRating((float) novel.getAverageRating());
            }
            if (ratingText != null) {
                ratingText.setText(novel.getFormattedRating());
            }

            // Load cover image with Glide
            if (novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(novel.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover)
                    .error(R.drawable.ic_logo)
                    .centerCrop()
                    .into(coverImage);
            } else {
                coverImage.setImageResource(R.drawable.img_placeholder_cover);
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    static class NovelDiffCallback extends DiffUtil.ItemCallback<Novel> {
        @Override
        public boolean areItemsTheSame(@NonNull Novel oldItem, @NonNull Novel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Novel oldItem, @NonNull Novel newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getTotalChapters() == newItem.getTotalChapters() &&
                   oldItem.getAverageRating() == newItem.getAverageRating();
        }
    }

    /**
     * Interface for item click callbacks
     */
    public interface OnItemClickListener {
        void onItemClick(Novel novel);
    }

    private NovelLongClickListener longClickListener;
    public void setOnLongClickListener(NovelLongClickListener l) { this.longClickListener = l; }

    public interface NovelLongClickListener {
        void onBookmark(Novel novel);
        void onAddToList(Novel novel);
        void onShare(Novel novel);
        void onReport(Novel novel);
    }

}
