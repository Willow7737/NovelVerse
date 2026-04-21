package com.novelverse.app.presentation.social;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/** Task 36: Leaderboard screen — Weekly / All-Time tabs, rank rows, pinned your-rank card */
@AndroidEntryPoint
public class LeaderboardActivity extends AppCompatActivity {

    @Inject UserPreferences         userPreferences;
    @Inject SupabaseDatabaseService dbService;

    private RecyclerView recycler;
    private TextView     yourRankText;
    private RankAdapter  adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recycler     = findViewById(R.id.leaderboard_recycler);
        yourRankText = findViewById(R.id.your_rank_text);

        adapter = new RankAdapter(userPreferences.getUserId());
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.leaderboard_tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                loadLeaderboard(tab.getPosition() == 0 ? "weekly" : "alltime");
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });

        loadLeaderboard("weekly");
    }

    private void loadLeaderboard(String period) {
        // Fetch from Supabase RPC get_reader_leaderboard(period)
        // Placeholder mock data until backend wired
        List<RankEntry> entries = new ArrayList<>();
        entries.add(new RankEntry(1, "shadow_quill",    "Shadow Quill",    null, 842));
        entries.add(new RankEntry(2, "moonreader99",    "Moon Reader",     null, 731));
        entries.add(new RankEntry(3, "loreseeker",      "Lore Seeker",     null, 690));
        entries.add(new RankEntry(4, "nightowl_reads",  "Night Owl",       null, 612));
        entries.add(new RankEntry(5, "fantasy_fox",     "Fantasy Fox",     null, 578));
        entries.add(new RankEntry(6, "chapter_hunter",  "Chapter Hunter",  null, 502));
        entries.add(new RankEntry(7, "scrollmaster",    "Scroll Master",   null, 444));
        entries.add(new RankEntry(8, "inkwhisperer",    "Ink Whisperer",   null, 390));
        entries.add(new RankEntry(9, "sageofstories",   "Sage of Stories", null, 321));
        entries.add(new RankEntry(10, "bookbound",      "Bookbound",       null, 290));
        adapter.setEntries(entries);

        // Pinned your-rank card
        String userId = userPreferences.getUserId();
        yourRankText.setText("You · #42 · 120 chapters");
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    static class RankAdapter extends RecyclerView.Adapter<RankAdapter.VH> {
        private List<RankEntry> entries = new ArrayList<>();
        private final String currentUserId;

        RankAdapter(String currentUserId) { this.currentUserId = currentUserId; }

        void setEntries(List<RankEntry> e) { entries = e; notifyDataSetChanged(); }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_leaderboard_row, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            RankEntry e = entries.get(pos);
            h.rank.setText(String.valueOf(e.rank));
            h.name.setText(e.displayName);
            h.score.setText(e.score + " ch");
            // Highlight current user's row
            boolean isMe = e.userId != null && e.userId.equals(currentUserId);
            h.itemView.setBackgroundColor(isMe ? 0x226366F1 : Color.TRANSPARENT);
            if (e.avatarUrl != null && !e.avatarUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(e.avatarUrl).circleCrop().into(h.avatar);
            }
        }

        @Override public int getItemCount() { return entries.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView rank, name, score; ImageView avatar;
            VH(View v) {
                super(v);
                rank   = v.findViewById(R.id.rank_number);
                name   = v.findViewById(R.id.display_name);
                score  = v.findViewById(R.id.score_text);
                avatar = v.findViewById(R.id.user_avatar);
            }
        }
    }

    static class RankEntry {
        int rank, score; String userId, displayName, avatarUrl;
        RankEntry(int rank, String userId, String displayName, String avatarUrl, int score) {
            this.rank = rank; this.userId = userId; this.displayName = displayName;
            this.avatarUrl = avatarUrl; this.score = score;
        }
    }
}
