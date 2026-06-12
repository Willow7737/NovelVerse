package com.novelverse.app.presentation.quests;

import android.content.Context;
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

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Quest;

public class QuestAdapter extends ListAdapter<Quest, QuestAdapter.ViewHolder> {

    public interface OnClaimListener {
        void onClaim(Quest quest);
    }

    private final OnClaimListener listener;

    private static final DiffUtil.ItemCallback<Quest> DIFF =
            new DiffUtil.ItemCallback<Quest>() {
                @Override
                public boolean areItemsTheSame(@NonNull Quest a, @NonNull Quest b) {
                    return a.getId().equals(b.getId());
                }
                @Override
                public boolean areContentsTheSame(@NonNull Quest a, @NonNull Quest b) {
                    return a.getCurrentValue() == b.getCurrentValue()
                            && a.isCompleted() == b.isCompleted()
                            && a.isRewardClaimed() == b.isRewardClaimed();
                }
            };

    public QuestAdapter(OnClaimListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView  iconView;
        private final TextView   titleText;
        private final TextView   descText;
        private final TextView   progressText;
        private final ProgressBar progressBar;
        private final TextView   rewardInkText;
        private final TextView   rewardXpText;
        private final TextView   rewardQuillText;
        private final TextView   claimBtn;
        private final View       completedBadge;

        ViewHolder(View v) {
            super(v);
            iconView        = v.findViewById(R.id.quest_icon);
            titleText       = v.findViewById(R.id.quest_title);
            descText        = v.findViewById(R.id.quest_description);
            progressText    = v.findViewById(R.id.quest_progress_text);
            progressBar     = v.findViewById(R.id.quest_progress_bar);
            rewardInkText   = v.findViewById(R.id.quest_reward_ink);
            rewardXpText    = v.findViewById(R.id.quest_reward_xp);
            rewardQuillText = v.findViewById(R.id.quest_reward_quill);
            claimBtn        = v.findViewById(R.id.quest_claim_btn);
            completedBadge  = v.findViewById(R.id.quest_completed_badge);
        }

        void bind(Quest q, OnClaimListener listener) {
            Context ctx = itemView.getContext();

            titleText.setText(q.getTitle());
            descText.setText(q.getDescription());

            // Progress
            int curr    = q.getCurrentValue();
            int target  = q.getTargetValue();
            int percent = q.getProgressPercent();

            progressBar.setProgress(percent);
            progressText.setText(curr + " / " + target);

            // Rewards row
            if (q.getRewardInk() > 0) {
                rewardInkText.setVisibility(View.VISIBLE);
                rewardInkText.setText("+" + q.getRewardInk() + " Ink");
            } else {
                rewardInkText.setVisibility(View.GONE);
            }

            if (q.getRewardXp() > 0) {
                rewardXpText.setVisibility(View.VISIBLE);
                rewardXpText.setText("+" + q.getRewardXp() + " XP");
            } else {
                rewardXpText.setVisibility(View.GONE);
            }

            if (q.getRewardQuill() > 0) {
                rewardQuillText.setVisibility(View.VISIBLE);
                rewardQuillText.setText("+" + q.getRewardQuill() + " Quill");
            } else {
                rewardQuillText.setVisibility(View.GONE);
            }

            // Icon – try to resolve drawable by name, fall back to default
            int iconRes = resolveIcon(ctx, q.getIconName());
            iconView.setImageResource(iconRes);

            // State
            if (q.isRewardClaimed()) {
                claimBtn.setVisibility(View.GONE);
                completedBadge.setVisibility(View.VISIBLE);
                progressBar.setAlpha(0.45f);
            } else if (q.isCompleted()) {
                claimBtn.setVisibility(View.VISIBLE);
                completedBadge.setVisibility(View.GONE);
                progressBar.setAlpha(1f);
                claimBtn.setOnClickListener(v -> listener.onClaim(q));
            } else {
                claimBtn.setVisibility(View.GONE);
                completedBadge.setVisibility(View.GONE);
                progressBar.setAlpha(1f);
            }
        }

        private int resolveIcon(Context ctx, String name) {
            if (name == null || name.isEmpty()) name = "ic_challenge";
            int res = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
            return res != 0 ? res : R.drawable.ic_challenge;
        }
    }
}
