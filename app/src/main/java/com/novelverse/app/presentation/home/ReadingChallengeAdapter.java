package com.novelverse.app.presentation.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.ReadingChallenge;

import java.util.concurrent.TimeUnit;

/** Task 37: Adapter for horizontal reading-challenge shelf */
public class ReadingChallengeAdapter extends ListAdapter<ReadingChallenge, ReadingChallengeAdapter.VH> {

    public ReadingChallengeAdapter() {
        super(new DiffUtil.ItemCallback<ReadingChallenge>() {
            @Override public boolean areItemsTheSame(@NonNull ReadingChallenge a, @NonNull ReadingChallenge b) {
                return a.getId() != null && a.getId().equals(b.getId());
            }
            @Override public boolean areContentsTheSame(@NonNull ReadingChallenge a, @NonNull ReadingChallenge b) {
                return a.getCurrentCount() == b.getCurrentCount() && a.isCompleted() == b.isCompleted();
            }
        });
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_reading_challenge, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ReadingChallenge c = getItem(pos);
        h.title.setText(c.getTitle());
        h.description.setText(c.getDescription());
        h.rewardPts.setText("+" + c.getRewardPoints() + " pts");

        int pct = (int) (c.getProgressFraction() * 100);
        h.progressBar.setProgress(pct);
        h.progressText.setText(c.getCurrentCount() + " / " + c.getTargetCount());

        if (c.getExpiresAt() != null) {
            long msLeft = c.getExpiresAt().getTime() - System.currentTimeMillis();
            long daysLeft = TimeUnit.MILLISECONDS.toDays(msLeft);
            h.expiry.setText(daysLeft > 0 ? "Resets in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s") : "Expires soon");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, description, rewardPts, progressText, expiry;
        ProgressBar progressBar;
        VH(View v) {
            super(v);
            title       = v.findViewById(R.id.challenge_title);
            description = v.findViewById(R.id.challenge_description);
            rewardPts   = v.findViewById(R.id.challenge_reward_pts);
            progressBar = v.findViewById(R.id.challenge_progress_bar);
            progressText= v.findViewById(R.id.challenge_progress_text);
            expiry      = v.findViewById(R.id.challenge_expiry);
        }
    }
}
