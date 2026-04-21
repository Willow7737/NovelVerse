package com.novelverse.app.presentation.reviews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.ReaderLevel;
import com.novelverse.app.domain.models.ReviewPost;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the full community reviews feed in {@link ReviewsActivity}.
 * Each item renders a social-media style post card with gamified user rank badge.
 */
public class ReviewFeedAdapter extends RecyclerView.Adapter<ReviewFeedAdapter.PostVH> {

    public interface OnItemClickListener {
        void onNovelClick(String novelId);
        void onLikeClick(ReviewPost post, int position);
    }

    private List<ReviewPost>   items    = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void submitList(List<ReviewPost> reviews) {
        this.items = reviews != null ? new ArrayList<>(reviews) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendList(List<ReviewPost> more) {
        if (more == null || more.isEmpty()) return;
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @NonNull
    @Override
    public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_post, parent, false);
        return new PostVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostVH h, int position) {
        h.bind(items.get(position), position, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class PostVH extends RecyclerView.ViewHolder {

        private final ImageView    avatar;
        private final TextView     displayName;
        private final TextView     rankBadge;
        private final TextView     username;
        private final TextView     timeAgo;
        private final TextView     genrePill;
        private final TextView     reviewText;
        private final LinearLayout novelCard;
        private final ImageView    novelCover;
        private final TextView     novelTitle;
        private final TextView     novelAuthor;
        private final TextView     novelRec;
        private final TextView     novelYear;
        private final ImageView    bookmarkIcon;
        private final LinearLayout starRow;
        private final TextView     likesCount;
        private final TextView     responsesCount;
        private final ImageView    heartBtn;

        PostVH(@NonNull View v) {
            super(v);
            avatar         = v.findViewById(R.id.post_avatar);
            displayName    = v.findViewById(R.id.post_display_name);
            rankBadge      = v.findViewById(R.id.post_rank_badge);
            username       = v.findViewById(R.id.post_username);
            timeAgo        = v.findViewById(R.id.post_time_ago);
            genrePill      = v.findViewById(R.id.post_genre_pill);
            reviewText     = v.findViewById(R.id.post_review_text);
            novelCard      = v.findViewById(R.id.post_novel_card);
            novelCover     = v.findViewById(R.id.post_novel_cover);
            novelTitle     = v.findViewById(R.id.post_novel_title);
            novelAuthor    = v.findViewById(R.id.post_novel_author);
            novelRec       = v.findViewById(R.id.post_novel_recommendations);
            novelYear      = v.findViewById(R.id.post_novel_year);
            bookmarkIcon   = v.findViewById(R.id.post_bookmark_icon);
            starRow        = v.findViewById(R.id.post_star_row);
            likesCount     = v.findViewById(R.id.post_likes_count);
            responsesCount = v.findViewById(R.id.post_responses_count);
            heartBtn       = v.findViewById(R.id.post_heart_btn);
        }

        void bind(ReviewPost p, int pos, OnItemClickListener listener) {
            Context ctx = itemView.getContext();

            // Avatar
            if (p.getUserAvatarUrl() != null && !p.getUserAvatarUrl().isEmpty()) {
                Glide.with(ctx).load(p.getUserAvatarUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.profile_default)
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.profile_default);
            }

            // Display name
            String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty())
                    ? p.getDisplayName() : p.getUsername();
            displayName.setText(name);

            // Rank badge
            ReviewSlideshowAdapter.applyRankBadge(ctx, rankBadge, p.getRank());

            // Handle + time + genre
            username.setText("@" + p.getUsername());
            timeAgo.setText(p.getTimeAgo());
            String genre = p.getNovelGenre();
            if (genre != null && !genre.isEmpty()) {
                genrePill.setText(genre.toLowerCase(java.util.Locale.US));
                genrePill.setVisibility(View.VISIBLE);
            } else {
                genrePill.setVisibility(View.GONE);
            }

            // Review text
            String text = p.getReviewText();
            if (text != null && !text.isEmpty()) {
                reviewText.setText(text);
                reviewText.setVisibility(View.VISIBLE);
            } else {
                reviewText.setVisibility(View.GONE);
            }

            // Novel cover
            int dp6 = Math.round(6 * ctx.getResources().getDisplayMetrics().density);
            if (p.getNovelCoverUrl() != null && !p.getNovelCoverUrl().isEmpty()) {
                Glide.with(ctx).load(p.getNovelCoverUrl())
                        .transform(new RoundedCorners(dp6))
                        .placeholder(R.drawable.ic_book_open)
                        .into(novelCover);
            } else {
                novelCover.setImageResource(R.drawable.ic_book_open);
            }

            novelTitle.setText(p.getNovelTitle() != null ? p.getNovelTitle() : "");
            novelAuthor.setText(p.getNovelAuthor() != null ? p.getNovelAuthor() : "");

            // Recommendation count pill
            String recText = ReviewPost.formatCount(p.getNovelRecommendations()) + " recommended";
            novelRec.setText(recText);

            // Year pill
            if (p.getNovelYear() > 0) {
                novelYear.setText(String.valueOf(p.getNovelYear()));
                novelYear.setVisibility(View.VISIBLE);
            } else {
                novelYear.setVisibility(View.GONE);
            }

            // Stars (14dp each)
            starRow.removeAllViews();
            int dp14 = Math.round(14 * ctx.getResources().getDisplayMetrics().density);
            int dp2  = Math.round(2  * ctx.getResources().getDisplayMetrics().density);
            for (int i = 0; i < 5; i++) {
                ImageView star = new ImageView(ctx);
                star.setImageResource(R.drawable.ic_star_filled);
                star.setColorFilter(i < p.getRating() ? 0xFFF59E0B : 0xFFCBD5E1);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp14, dp14);
                lp.setMarginEnd(dp2);
                star.setLayoutParams(lp);
                starRow.addView(star);
            }

            // Likes
            int likes = p.getLikesCount();
            likesCount.setText(ReviewPost.formatCount(likes) + (likes == 1 ? " like" : " likes"));

            // Responses
            int resp = p.getResponsesCount();
            responsesCount.setText(
                    ReviewPost.formatCount(resp) + (resp == 1 ? " response" : " responses"));

            // Heart icon state
            applyHeartState(ctx, p.isLiked());

            // Click: novel card
            novelCard.setOnClickListener(v -> {
                if (listener != null && p.getNovelId() != null) {
                    listener.onNovelClick(p.getNovelId());
                }
            });

            // Click: heart toggle
            heartBtn.setOnClickListener(v -> {
                p.setLiked(!p.isLiked());
                p.setLikesCount(p.getLikesCount() + (p.isLiked() ? 1 : -1));
                applyHeartState(ctx, p.isLiked());
                int newLikes = p.getLikesCount();
                likesCount.setText(ReviewPost.formatCount(newLikes)
                        + (newLikes == 1 ? " like" : " likes"));
                if (listener != null) listener.onLikeClick(p, pos);
            });

            // Bookmark icon (visual only for now — shows bookmarked state)
            bookmarkIcon.setImageResource(R.drawable.ic_bookmark);
            bookmarkIcon.setColorFilter(ctx.getColor(R.color.text_tertiary));
        }

        private void applyHeartState(Context ctx, boolean liked) {
            if (liked) {
                heartBtn.setImageResource(R.drawable.ic_like_filled);
                heartBtn.setColorFilter(0xFFFF453A); // iOS red
            } else {
                heartBtn.setImageResource(R.drawable.ic_like_outline);
                heartBtn.setColorFilter(ctx.getColor(R.color.text_tertiary));
            }
        }
    }
}
