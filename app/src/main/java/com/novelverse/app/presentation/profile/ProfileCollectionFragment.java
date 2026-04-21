package com.novelverse.app.presentation.profile;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.domain.gamification.XpLevelEngine;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileCollectionFragment extends Fragment {

    @Inject UserPreferences prefs;
    private GamificationViewModel vm;

    // Avatar frame asset names in order
    private static final String[] AVATAR_FRAMES = {
        "avatar_frame_reader.png", "avatar_frame_writer.png",
        "avatar_frame_bronze.png", "avatar_frame_silver.png",
        "avatar_frame_gold.png",   "avatar_frame_flame_streak.png",
        "avatar_frame_legendary.png"
    };

    // Theme asset names in order
    private static final String[] THEMES = {
        "theme_fantasy_forest.png", "theme_scifi_circuit.png",
        "theme_romance.png",        "theme_ocean.png",
        "theme_galaxy.png",         "theme_premium_gold.png"
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);

        String userId = prefs.getUserId();
        if (userId != null) vm.init(userId);

        // Avatar frames horizontal RecyclerView
        RecyclerView framesRv = view.findViewById(R.id.avatar_frames_recycler);
        if (framesRv != null) {
            framesRv.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            framesRv.setAdapter(new AssetImageAdapter(AVATAR_FRAMES, 80));
        }

        // Themes horizontal RecyclerView
        RecyclerView themesRv = view.findViewById(R.id.themes_recycler);
        if (themesRv != null) {
            themesRv.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            themesRv.setAdapter(new AssetImageAdapter(THEMES, 120));
        }

        // Badge slots — reactive to level
        vm.getLevel().observe(getViewLifecycleOwner(), lv -> updateBadgeSlots(view, lv));
    }

    private void updateBadgeSlots(View root, UserLevelEntity lv) {
        if (lv == null) return;
        int level = lv.getCurrentLevel();
        int slots = XpLevelEngine.badgeSlotsForLevel(level);

        FrameLayout slot2 = root.findViewById(R.id.badge_slot_2);
        FrameLayout slot3 = root.findViewById(R.id.badge_slot_3);

        if (slot2 != null) {
            boolean unlocked2 = slots >= 2;
            slot2.setAlpha(unlocked2 ? 1f : 0.4f);
            ImageView lock2 = root.findViewById(R.id.badge_slot_2_lock);
            if (lock2 != null) lock2.setVisibility(unlocked2 ? View.GONE : View.VISIBLE);
        }
        if (slot3 != null) {
            boolean unlocked3 = slots >= 3;
            slot3.setAlpha(unlocked3 ? 1f : 0.4f);
            ImageView lock3 = root.findViewById(R.id.badge_slot_3_lock);
            if (lock3 != null) lock3.setVisibility(unlocked3 ? View.GONE : View.VISIBLE);
        }
    }

    // ── Simple adapter for horizontal asset image grids ───────────────────────

    private class AssetImageAdapter extends RecyclerView.Adapter<AssetImageAdapter.VH> {
        private final String[] paths;
        private final int sizeDp;

        AssetImageAdapter(String[] paths, int sizeDp) {
            this.paths  = paths;
            this.sizeDp = sizeDp;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int px = (int)(sizeDp * parent.getContext().getResources().getDisplayMetrics().density);
            ImageView iv = new ImageView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(px, px);
            lp.setMarginEnd((int)(10 * parent.getContext().getResources().getDisplayMetrics().density));
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                AssetManager assets = requireContext().getAssets();
                InputStream is = assets.open("gamification_icons/" + paths[pos]);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                h.iv.setImageBitmap(bmp);
            } catch (IOException ignored) {}
        }

        @Override public int getItemCount() { return paths.length; }

        class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            VH(View v) { super(v); iv = (ImageView) v; }
        }
    }
}
