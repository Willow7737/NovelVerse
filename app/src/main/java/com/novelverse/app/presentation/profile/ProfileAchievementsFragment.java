package com.novelverse.app.presentation.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileAchievementsFragment extends Fragment {

    @Inject UserPreferences prefs;

    private GamificationViewModel vm;
    private AchievementCardAdapter adapter;
    private String activeFilter = "ALL";

    private List<AchievementEntity> fullCatalog = new ArrayList<>();
    private List<UserAchievementEntity> userStates = new ArrayList<>();

    // Filter chip views
    private TextView chipAll;
    private TextView chipReader;
    private TextView chipWriter;
    private TextView chipStreak;
    private TextView chipSocial;
    private TextView chipSupporter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);

        String userId = prefs.getUserId();
        if (userId != null) vm.init(userId);

        // RecyclerView
        RecyclerView rv = view.findViewById(R.id.achievements_recycler);
        adapter = new AchievementCardAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Bind chip TextViews
        chipAll       = view.findViewById(R.id.chip_all);
        chipReader    = view.findViewById(R.id.chip_reader);
        chipWriter    = view.findViewById(R.id.chip_writer);
        chipStreak    = view.findViewById(R.id.chip_streak);
        chipSocial    = view.findViewById(R.id.chip_social);
        chipSupporter = view.findViewById(R.id.chip_supporter);

        // Wire chip clicks
        chipAll      .setOnClickListener(v -> selectChip("ALL"));
        chipReader   .setOnClickListener(v -> selectChip("READER"));
        chipWriter   .setOnClickListener(v -> selectChip("WRITER"));
        chipStreak   .setOnClickListener(v -> selectChip("STREAK"));
        chipSocial   .setOnClickListener(v -> selectChip("SOCIAL"));
        chipSupporter.setOnClickListener(v -> selectChip("SUPPORTER"));

        // Ensure visual state matches activeFilter on first load
        refreshChipVisuals();

        // Observe catalog (List<AchievementEntity>)
        vm.getCatalog().observe(getViewLifecycleOwner(), catalog -> {
            fullCatalog = catalog != null ? catalog : new ArrayList<>();
            applyFilter();
        });

        // Observe user achievement states (List<UserAchievementEntity>)
        vm.getAllAchievements().observe(getViewLifecycleOwner(), states -> {
            userStates = states != null ? states : new ArrayList<>();
            adapter.setUserStates(userStates);
        });
    }

    // ── Chip selection ────────────────────────────────────────────────────────

    private void selectChip(String filter) {
        activeFilter = filter;
        refreshChipVisuals();
        applyFilter();
    }

    private void refreshChipVisuals() {
        setChipState(chipAll,       "ALL");
        setChipState(chipReader,    "READER");
        setChipState(chipWriter,    "WRITER");
        setChipState(chipStreak,    "STREAK");
        setChipState(chipSocial,    "SOCIAL");
        setChipState(chipSupporter, "SUPPORTER");
    }

    private void setChipState(TextView chip, String filter) {
        if (chip == null) return;
        boolean active = activeFilter.equals(filter);
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(requireContext().getColor(
                active ? R.color.chip_selected_text : R.color.chip_unselected_text));
    }

    // ── Filter (original logic, unchanged) ───────────────────────────────────

    private void applyFilter() {
        if (fullCatalog == null) return;
        List<AchievementEntity> filtered = new ArrayList<>();
        for (AchievementEntity e : fullCatalog) {
            if ("ALL".equals(activeFilter) || activeFilter.equals(e.getCategory())) {
                filtered.add(e);
            }
        }
        adapter.submitList(filtered);
    }
}
