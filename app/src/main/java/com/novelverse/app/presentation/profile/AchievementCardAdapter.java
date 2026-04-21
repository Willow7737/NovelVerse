package com.novelverse.app.presentation.profile;

import android.graphics.Color;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.domain.gamification.AchievementDefinitions;
import com.novelverse.app.utils.GameAssets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Achievement list adapter.
 * Loads badge images and rarity card frames from the Supabase game-assets
 * bucket via Glide + GameAssets. No local AssetManager reads.
 */
public class AchievementCardAdapter extends ListAdapter<AchievementEntity, AchievementCardAdapter.VH> {

    private Map<String, UserAchievementEntity> userStateMap = new HashMap<>();
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    public AchievementCardAdapter() { super(DIFF); }

    private static final DiffUtil.ItemCallback<AchievementEntity> DIFF =
        new DiffUtil.ItemCallback<AchievementEntity>() {
            @Override public boolean areItemsTheSame(@NonNull AchievementEntity a, @NonNull AchievementEntity b) {
                return a.getId().equals(b.getId());
            }
            @Override public boolean areContentsTheSame(@NonNull AchievementEntity a, @NonNull AchievementEntity b) {
                return a.getId().equals(b.getId());
            }
        };

    /** Call when user achievement states change — triggers a full redraw. */
    public void setUserStates(List<UserAchievementEntity> states) {
        userStateMap = new HashMap<>();
        if (states != null) {
            for (UserAchievementEntity ua : states) userStateMap.put(ua.getAchievementId(), ua);
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AchievementEntity ach = getItem(position);
        UserAchievementEntity ua = userStateMap.get(ach.getId());
        boolean unlocked = (ua != null && ua.isUnlocked());

        // Title
        h.title.setText(ach.getTitle());

        // Description: hidden for invisible surprise achievements until unlocked
        h.desc.setText((!ach.isVisible() && !unlocked) ? "???" : ach.getDescription());

        // Rarity chip colour
        h.rarityChip.setText(ach.getRarity());
        try {
            h.rarityChip.setBackgroundColor(
                Color.parseColor(AchievementDefinitions.rarityColor(ach.getRarity())));
        } catch (Exception ignored) {}

        // Badge image — load from Supabase
        Glide.with(h.itemView.getContext())
            .load(GameAssets.getAchievementUrl(ach.getId()))
            .diskCacheStrategy(DiskCacheStrategy.ALL) // static assets: cache forever
            .placeholder(R.drawable.ic_challenge)
            .error(R.drawable.ic_challenge)
            .into(h.badge);

        // Rarity frame — load from Supabase
        Glide.with(h.itemView.getContext())
            .load(GameAssets.getRarityFrameUrl(ach.getRarity()))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(h.cardBg);

        // Locked overlay
        h.lockedOverlay.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        h.lockedIcon.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        h.itemView.setAlpha(unlocked ? 1f : 0.65f);

        // Progress bar (visible, in-progress achievements only)
        if (ach.isVisible() && !unlocked && ach.getTargetValue() > 1) {
            h.progressContainer.setVisibility(View.VISIBLE);
            int current = (ua != null) ? ua.getCurrentProgress() : 0;
            h.progressBar.setProgress((int)(100f * current / ach.getTargetValue()));
            h.progressText.setText(current + "/" + ach.getTargetValue());
        } else {
            h.progressContainer.setVisibility(View.GONE);
        }

        // Rewards
        setReward(h.rewardXp,    ach.getXpReward(),    " XP");
        setReward(h.rewardInk,   ach.getInkReward(),   " Ink");
        setReward(h.rewardQuill, ach.getQuillReward(), " Quill");

        // Unlock date
        if (unlocked && ua.getUnlockedAt() > 0) {
            h.unlockedDate.setText("Unlocked " + DATE_FMT.format(new Date(ua.getUnlockedAt())));
            h.unlockedDate.setVisibility(View.VISIBLE);
        } else {
            h.unlockedDate.setVisibility(View.GONE);
        }
    }

    private static void setReward(TextView tv, int amount, String suffix) {
        if (amount > 0) { tv.setText("+" + amount + suffix); tv.setVisibility(View.VISIBLE); }
        else            { tv.setVisibility(View.GONE); }
    }

    @Override
    public void onViewRecycled(@NonNull VH h) {
        super.onViewRecycled(h);
        Glide.with(h.itemView.getContext()).clear(h.badge);
        Glide.with(h.itemView.getContext()).clear(h.cardBg);
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView   badge, cardBg, lockedIcon;
        final View        lockedOverlay, progressContainer;
        final TextView    title, desc, rarityChip;
        final TextView    rewardXp, rewardInk, rewardQuill;
        final TextView    progressText, unlockedDate;
        final ProgressBar progressBar;

        VH(View v) {
            super(v);
            badge             = v.findViewById(R.id.achievement_badge);
            cardBg            = v.findViewById(R.id.card_bg_rarity);
            lockedOverlay     = v.findViewById(R.id.locked_overlay);
            lockedIcon        = v.findViewById(R.id.locked_icon);
            title             = v.findViewById(R.id.achievement_title);
            desc              = v.findViewById(R.id.achievement_desc);
            rarityChip        = v.findViewById(R.id.rarity_chip);
            rewardXp          = v.findViewById(R.id.reward_xp);
            rewardInk         = v.findViewById(R.id.reward_ink);
            rewardQuill       = v.findViewById(R.id.reward_quill);
            progressBar       = v.findViewById(R.id.achievement_progress_bar);
            progressText      = v.findViewById(R.id.progress_text);
            progressContainer = v.findViewById(R.id.progress_container);
            unlockedDate      = v.findViewById(R.id.unlocked_date);
        }
    }
}
