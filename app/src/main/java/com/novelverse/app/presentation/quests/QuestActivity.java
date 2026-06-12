package com.novelverse.app.presentation.quests;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseClient;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.data.repository.QuestRepository;
import com.novelverse.app.domain.models.Quest;
import com.novelverse.app.presentation.common.views.DottedDividerDecoration;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuestActivity extends AppCompatActivity {

    private static final String TAG = "QuestActivity";

    // ── Injected ──────────────────────────────────────────────────────────
    @Inject
    SupabaseClient supabaseClient;

    // ── Views ─────────────────────────────────────────────────────────────
    private View          tabDaily, tabWeekly, tabOneTime;
    private TextView      tabDailyText, tabWeeklyText, tabOneTimeText;
    private View          tabIndicatorDaily, tabIndicatorWeekly, tabIndicatorOneTime;

    private RecyclerView  recyclerView;
    private View          shimmerView;
    private View          emptyView;

    // ── Data ──────────────────────────────────────────────────────────────
    private QuestAdapter  adapter;
    private QuestRepository repository;
    private UserPreferences prefs;
    private String        currentTab = "daily";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: status bar transparent, drawn behind
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_quests);

        prefs = new UserPreferences(this);

        // Supabase — client is injected by Hilt; wire the service & repo here
        SupabaseDatabaseService dbService = new SupabaseDatabaseService(supabaseClient);
        repository = new QuestRepository(dbService);

        bindViews();
        setupTabs();
        setupRecycler();
        setupBack();

        loadTab("daily");
    }

    // ── Bind ──────────────────────────────────────────────────────────────
    private void bindViews() {
        tabDaily          = findViewById(R.id.tab_daily);
        tabWeekly         = findViewById(R.id.tab_weekly);
        tabOneTime        = findViewById(R.id.tab_one_time);
        tabDailyText      = findViewById(R.id.tab_daily_text);
        tabWeeklyText     = findViewById(R.id.tab_weekly_text);
        tabOneTimeText    = findViewById(R.id.tab_one_time_text);
        tabIndicatorDaily    = findViewById(R.id.tab_indicator_daily);
        tabIndicatorWeekly   = findViewById(R.id.tab_indicator_weekly);
        tabIndicatorOneTime  = findViewById(R.id.tab_indicator_one_time);
        recyclerView      = findViewById(R.id.quests_recycler);
        shimmerView       = findViewById(R.id.quests_shimmer);
        emptyView         = findViewById(R.id.quests_empty);
    }

    // ── Back ──────────────────────────────────────────────────────────────
    private void setupBack() {
        View backBtn = findViewById(R.id.btn_back);
        if (backBtn != null) backBtn.setOnClickListener(v -> onBackPressed());
    }

    // ── Tabs ──────────────────────────────────────────────────────────────
    private void setupTabs() {
        tabDaily.setOnClickListener(v -> { if (!currentTab.equals("daily"))   loadTab("daily"); });
        tabWeekly.setOnClickListener(v -> { if (!currentTab.equals("weekly")) loadTab("weekly"); });
        tabOneTime.setOnClickListener(v -> { if (!currentTab.equals("one_time")) loadTab("one_time"); });
    }

    private void setActiveTab(String tab) {
        currentTab = tab;

        // Reset all
        int inactive = getColor(R.color.text_secondary);
        int active   = getColor(R.color.primary);
        tabDailyText.setTextColor(inactive);
        tabWeeklyText.setTextColor(inactive);
        tabOneTimeText.setTextColor(inactive);
        tabIndicatorDaily.setVisibility(View.INVISIBLE);
        tabIndicatorWeekly.setVisibility(View.INVISIBLE);
        tabIndicatorOneTime.setVisibility(View.INVISIBLE);

        switch (tab) {
            case "daily":
                tabDailyText.setTextColor(active);
                tabIndicatorDaily.setVisibility(View.VISIBLE);
                break;
            case "weekly":
                tabWeeklyText.setTextColor(active);
                tabIndicatorWeekly.setVisibility(View.VISIBLE);
                break;
            case "one_time":
                tabOneTimeText.setTextColor(active);
                tabIndicatorOneTime.setVisibility(View.VISIBLE);
                break;
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────
    private void setupRecycler() {
        adapter = new QuestAdapter(quest -> claimReward(quest));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DottedDividerDecoration(this));
    }

    // ── Load quests ───────────────────────────────────────────────────────
    private void loadTab(String tab) {
        setActiveTab(tab);
        showLoading(true);
        emptyView.setVisibility(View.GONE);

        String userId = prefs.getUserId();
        String token  = prefs.getAccessToken();

        repository.loadQuestsWithProgress(tab, userId, token,
                new QuestRepository.QuestCallback() {
                    @Override
                    public void onSuccess(List<Quest> quests) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            if (quests.isEmpty()) {
                                emptyView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                emptyView.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                adapter.submitList(quests);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(QuestActivity.this,
                                    "Couldn't load quests. Try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // ── Claim reward ──────────────────────────────────────────────────────
    private void claimReward(Quest quest) {
        String userId = prefs.getUserId();
        String token  = prefs.getAccessToken();

        repository.claimReward(quest.getId(), userId, token,
                new QuestRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        mainHandler.post(() -> {
                            String msg = "Reward claimed!";
                            if (quest.getRewardInk() > 0)   msg += "  +" + quest.getRewardInk()   + " Ink";
                            if (quest.getRewardXp() > 0)    msg += "  +" + quest.getRewardXp()    + " XP";
                            if (quest.getRewardQuill() > 0) msg += "  +" + quest.getRewardQuill() + " Quill";
                            Toast.makeText(QuestActivity.this, msg, Toast.LENGTH_LONG).show();
                            loadTab(currentTab); // refresh list
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() ->
                                Toast.makeText(QuestActivity.this,
                                        "Failed to claim reward. Try again.", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // ── UI helpers ────────────────────────────────────────────────────────
    private void showLoading(boolean loading) {
        shimmerView.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE   : View.VISIBLE);
    }
}
