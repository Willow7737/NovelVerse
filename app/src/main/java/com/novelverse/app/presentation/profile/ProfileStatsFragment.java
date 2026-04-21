package com.novelverse.app.presentation.profile;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.domain.gamification.XpLevelEngine;
import com.novelverse.app.presentation.common.views.StreakWidgetView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileStatsFragment extends Fragment {

    @Inject UserPreferences prefs;

    private GamificationViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);

        String userId = prefs.getUserId();
        if (userId != null) vm.init(userId);

        bindLevelCard(view);
        bindCurrencyCard(view);
        bindStreakCard(view);
        bindAchievementCount(view);
    }

    private void bindLevelCard(View root) {
        ImageView  levelBadge = root.findViewById(R.id.level_badge_image);
        ImageView  xpStar     = root.findViewById(R.id.xp_star_icon);
        TextView   levelTitle = root.findViewById(R.id.level_title);
        TextView   xpLabel    = root.findViewById(R.id.xp_label);
        ProgressBar xpBar     = root.findViewById(R.id.xp_progress_bar);
        TextView   perkHint   = root.findViewById(R.id.next_level_perk);

        loadAsset(levelBadge, "gamification_icons/level_badge_frame.png");
        loadAsset(xpStar,     "gamification_icons/xp_star.png");

        vm.getLevel().observe(getViewLifecycleOwner(), lv -> {
            if (lv == null) return;
            levelTitle.setText("Level " + lv.getCurrentLevel());
            xpLabel.setText(lv.getXpInLevel() + " / " + lv.getXpLevelTarget() + " XP");
            int pct = lv.getXpLevelTarget() > 0
                ? (int)(100f * lv.getXpInLevel() / lv.getXpLevelTarget()) : 0;
            xpBar.setProgress(pct);

            // Next perk hint
            if (perkHint != null) {
                int lvl = lv.getCurrentLevel();
                if (lvl < 10)      perkHint.setText("Level 10 unlocks 2nd badge slot");
                else if (lvl < 25) perkHint.setText("Level 25 unlocks 3rd badge slot + animated avatars");
                else if (lvl < 50) perkHint.setText("Level 50 unlocks Veteran title + exclusive theme");
                else               perkHint.setText("Veteran — maximum perks unlocked! 🎖");
            }
        });
    }

    private void bindCurrencyCard(View root) {
        ImageView inkImg   = root.findViewById(R.id.ink_bottle_image);
        ImageView quillImg = root.findViewById(R.id.quill_image);
        TextView  inkBal   = root.findViewById(R.id.ink_balance);
        TextView  quillBal = root.findViewById(R.id.quill_balance);

        loadAsset(inkImg,   "gamification_icons/ink_bottle.png");
        loadAsset(quillImg, "gamification_icons/golden_quill.png");

        vm.getCurrency().observe(getViewLifecycleOwner(), cur -> {
            if (cur == null) return;
            inkBal.setText(formatBalance(cur.getInkBalance()));
            quillBal.setText(formatBalance(cur.getQuillBalance()));
        });
    }

    private void bindStreakCard(View root) {
        StreakWidgetView widget  = root.findViewById(R.id.streak_widget);
        Button btnFreeze         = root.findViewById(R.id.btn_freeze);
        Button btnShield         = root.findViewById(R.id.btn_shield);
        TextView longestText     = root.findViewById(R.id.streak_longest);
        long now = System.currentTimeMillis();

        vm.getStreak().observe(getViewLifecycleOwner(), streak -> {
            if (streak == null) return;
            widget.bind(streak, now);
            longestText.setText("Longest streak: " + streak.getLongestStreak() + " days");

            // Freeze button label
            if (btnFreeze != null) {
                btnFreeze.setText(vm.hasFreeFreezeAvailable()
                    ? "❄ Free Freeze" : "❄ Freeze (50 Ink)");
            }
            // Shield button label
            if (btnShield != null) {
                btnShield.setText(vm.shieldFreeForCurrentStreak()
                    ? "🛡 Free Shield" : "🛡 Shield (150 Quill)");
            }
        });

        if (btnFreeze != null) {
            btnFreeze.setOnClickListener(v -> {
                boolean useFree = vm.hasFreeFreezeAvailable();
                vm.applyFreeze(useFree);
            });
        }
        if (btnShield != null) {
            btnShield.setOnClickListener(v -> {
                boolean isFree = vm.shieldFreeForCurrentStreak();
                UserStreakEntity s = vm.getStreak().getValue();
                int recoveredStreak = s != null ? s.getLongestStreak() : 0;
                vm.applyShieldRecovery(recoveredStreak, isFree);
            });
        }

        // Toast feedback
        vm.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindAchievementCount(View root) {
        TextView countText = root.findViewById(R.id.achievements_count);
        vm.getAllAchievements().observe(getViewLifecycleOwner(), list -> {
            int unlocked = 0;
            if (list != null) {
                for (com.novelverse.app.data.local.entities.UserAchievementEntity ua : list)
                    if (ua.isUnlocked()) unlocked++;
            }
            countText.setText(unlocked + " / 28");
        });
    }

    private void loadAsset(ImageView iv, String path) {
        if (iv == null || getContext() == null) return;
        try {
            AssetManager assets = requireContext().getAssets();
            InputStream is = assets.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {}
    }

    private String formatBalance(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000f);
        return String.valueOf(v);
    }
}
