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
import com.novelverse.app.R;
import com.novelverse.app.domain.models.ReaderLevel;
import com.novelverse.app.domain.models.ReviewPost;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2 adapter that shows compact review slides on the home screen.
 * Each slide displays the reviewer's avatar, display name, rank badge,
 * a review excerpt, and the novel title it belongs to.
 */
public class ReviewSlideshowAdapter extends RecyclerView.Adapter<ReviewSlideshowAdapter.SlideVH> {

    private List<ReviewPost> items = new ArrayList<>();

    public void submitList(List<ReviewPost> reviews) {
        this.items = reviews != null ? new ArrayList<>(reviews) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlideVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_slide, parent, false);
        return new SlideVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideVH h, int position) {
        h.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class SlideVH extends RecyclerView.ViewHolder {

        private final ImageView      avatar;
        private final TextView       displayName;
        private final TextView       username;
        private final TextView       rankBadge;
        private final LinearLayout   starRow;
        private final TextView       reviewText;
        private final TextView       novelTitle;
        private final TextView       genre;

        SlideVH(@NonNull View v) {
            super(v);
            avatar      = v.findViewById(R.id.slide_avatar);
            displayName = v.findViewById(R.id.slide_display_name);
            username    = v.findViewById(R.id.slide_username);
            rankBadge   = v.findViewById(R.id.slide_rank_badge);
            starRow     = v.findViewById(R.id.slide_star_row);
            reviewText  = v.findViewById(R.id.slide_review_text);
            novelTitle  = v.findViewById(R.id.slide_novel_title);
            genre       = v.findViewById(R.id.slide_genre);
        }

        void bind(ReviewPost p) {
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

            // Name
            displayName.setText(p.getDisplayName() != null && !p.getDisplayName().isEmpty()
                    ? p.getDisplayName() : p.getUsername());
            username.setText("@" + p.getUsername());

            // Rank badge
            applyRankBadge(ctx, rankBadge, p.getRank());

            // Stars (small, 13dp)
            starRow.removeAllViews();
            int dp13 = Math.round(13 * ctx.getResources().getDisplayMetrics().density);
            int dp2  = Math.round(2  * ctx.getResources().getDisplayMetrics().density);
            for (int i = 0; i < 5; i++) {
                ImageView star = new ImageView(ctx);
                star.setImageResource(R.drawable.ic_star_filled);
                star.setColorFilter(i < p.getRating() ? 0xFFF59E0B : 0xFFCBD5E1);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp13, dp13);
                lp.setMarginEnd(dp2);
                star.setLayoutParams(lp);
                starRow.addView(star);
            }

            // Review text excerpt
            String text = p.getReviewText();
            if (text != null && !text.isEmpty()) {
                reviewText.setText(text);
                reviewText.setVisibility(View.VISIBLE);
            } else {
                reviewText.setVisibility(View.GONE);
            }

            // Novel title
            novelTitle.setText(p.getNovelTitle() != null ? p.getNovelTitle() : "");

            // Genre pill
            if (p.getNovelGenre() != null && !p.getNovelGenre().isEmpty()) {
                genre.setText(p.getNovelGenre().toLowerCase(java.util.Locale.US));
                genre.setVisibility(View.VISIBLE);
            } else {
                genre.setVisibility(View.GONE);
            }
        }
    }

    // ── Static rank badge helper (shared with ReviewFeedAdapter) ──────────

    public static void applyRankBadge(Context ctx, TextView badge, ReaderLevel.Level rank) {
        badge.setText(rank.name);
        switch (rank) {
            case BOOKWORM:
                badge.setBackground(ctx.getDrawable(R.drawable.bg_rank_bookworm));
                badge.setTextColor(0xFF2E7D32);
                break;
            case SCHOLAR:
                badge.setBackground(ctx.getDrawable(R.drawable.bg_rank_scholar));
                badge.setTextColor(0xFF1565C0);
                break;
            case SAGE:
                badge.setBackground(ctx.getDrawable(R.drawable.bg_rank_sage));
                badge.setTextColor(0xFF6A1B9A);
                break;
            case LOREKEEPER:
                badge.setBackground(ctx.getDrawable(R.drawable.bg_rank_lorekeeper));
                badge.setTextColor(0xFFB45309);
                break;
            case ORACLE:
                badge.setBackground(ctx.getDrawable(R.drawable.bg_rank_oracle));
                badge.setTextColor(0xFFC2410C);
                break;
        }
    }
}
