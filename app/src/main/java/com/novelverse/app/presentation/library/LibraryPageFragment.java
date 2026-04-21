package com.novelverse.app.presentation.library;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novelverse.app.R;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.common.adapters.NovelAdapter;
import com.novelverse.app.presentation.novel.detail.NovelDetailActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A single page inside the library {@link LibraryFragment} ViewPager2.
 *
 * <p>Each page represents one library category (reading, completed, etc.)
 * and is responsible for:
 * <ul>
 *   <li>Fetching its own Supabase data.</li>
 *   <li>Rendering items with a borderless row layout separated by dotted dividers.</li>
 *   <li>Showing the appropriate empty state.</li>
 * </ul>
 */
@AndroidEntryPoint
public class LibraryPageFragment extends Fragment {

    private static final String TAG         = "LibraryPage";
    private static final String ARG_STATUS  = "status";
    private static final String ARG_PAGE    = "page";

    @Inject SupabaseDatabaseService dbService;
    @Inject UserPreferences         userPreferences;

    private String status;
    private int    page;

    private RecyclerView  recyclerView;
    private LinearLayout  emptyLayout;
    private TextView      emptyTitle;
    private TextView      emptySubtitle;

    private NovelAdapter adapter;
    private final Gson   gson = new Gson();

    // ── Factory ──────────────────────────────────────────────────────────────

    public static LibraryPageFragment newInstance(String status, int page) {
        LibraryPageFragment f = new LibraryPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        args.putInt(ARG_PAGE, page);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString(ARG_STATUS, "reading");
            page   = getArguments().getInt(ARG_PAGE, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // We reuse the same outer shell as before; only the item layout changes.
        // If you have a separate page_library_fragment.xml you can inflate that here.
        // For now we inflate a minimal inline view tree.
        return inflater.inflate(R.layout.fragment_library_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView  = view.findViewById(R.id.page_library_rv);
        emptyLayout   = view.findViewById(R.id.page_library_empty);
        emptyTitle    = view.findViewById(R.id.page_library_empty_title);
        emptySubtitle = view.findViewById(R.id.page_library_empty_subtitle);

        View cta = view.findViewById(R.id.page_library_empty_cta);
        if (cta != null) {
            cta.setOnClickListener(v -> {
                if (getActivity() instanceof com.novelverse.app.presentation.home.HomeActivity)
                    ((com.novelverse.app.presentation.home.HomeActivity) getActivity())
                            .navigateToIndex(0);
            });
        }

        // Borderless rows + dotted-line separators
        adapter = new NovelAdapter(NovelAdapter.VIEW_TYPE_LIBRARY);
        adapter.setOnItemClickListener(novel -> {
            Intent i = new Intent(requireContext(), NovelDetailActivity.class);
            i.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
            startActivity(i);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new com.novelverse.app.presentation.common.views.DottedDividerDecoration(
                        requireContext()));

        // Wire nav-bar scroll-hide
        if (getActivity() instanceof com.novelverse.app.presentation.home.HomeActivity) {
            ((com.novelverse.app.presentation.home.HomeActivity) getActivity())
                    .attachNavToRecyclerView(recyclerView);
        }

        setEmptyLabels(page);
        loadData();
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadData() {
        String userId = userPreferences.getUserId();
        String token  = userPreferences.getAccessToken();

        if (userId == null) { showEmpty(true); return; }

        adapter.submitList(null);
        showEmpty(false);

        String filter = "user_id=eq." + userId
                + (isRealStatus(status) ? "&status=eq." + status : "");
        String select = "novels(*,profiles!author_id(display_name,avatar_url,username))";
        String url    = dbService.buildSelectUrl("user_library", select, filter, "created_at.desc");

        dbService.rawSelect(url, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                List<Novel> novels = parse(result);
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    adapter.submitList(novels);
                    showEmpty(novels.isEmpty());
                });
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Log.e(TAG, "Library fetch error [" + status + "]: " + error);
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    showEmpty(true);
                });
            }
        });
    }

    private boolean isRealStatus(String s) {
        return !"favorites".equals(s) && !"downloads".equals(s);
    }

    private List<Novel> parse(String json) {
        List<Novel> list = new ArrayList<>();
        try {
            JsonArray rows = gson.fromJson(json, JsonArray.class);
            if (rows == null) return list;
            for (int i = 0; i < rows.size(); i++) {
                JsonObject row = rows.get(i).getAsJsonObject();
                if (!row.has("novels") || row.get("novels").isJsonNull()) continue;
                JsonObject novelObj = row.getAsJsonObject("novels");
                Novel novel = gson.fromJson(novelObj, Novel.class);
                if (novelObj.has("profiles") && !novelObj.get("profiles").isJsonNull()) {
                    JsonObject prof = novelObj.getAsJsonObject("profiles");
                    if (prof.has("display_name") && !prof.get("display_name").isJsonNull())
                        novel.setAuthorDisplayName(prof.get("display_name").getAsString());
                    if (prof.has("avatar_url") && !prof.get("avatar_url").isJsonNull())
                        novel.setAuthorAvatarUrl(prof.get("avatar_url").getAsString());
                    if (prof.has("username") && !prof.get("username").isJsonNull())
                        novel.setAuthorUsername(prof.get("username").getAsString());
                }
                list.add(novel);
            }
        } catch (Exception e) {
            Log.e(TAG, "parse error: " + e.getMessage());
        }
        return list;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showEmpty(boolean empty) {
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE  : View.VISIBLE);
        if (emptyLayout  != null) emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void setEmptyLabels(int index) {
        if (emptyTitle == null || emptySubtitle == null) return;
        switch (index) {
            case 0: emptyTitle.setText("Nothing in progress");
                    emptySubtitle.setText("Novels you're reading will appear here"); break;
            case 1: emptyTitle.setText("No completed novels");
                    emptySubtitle.setText("Finish a novel and it'll show up here"); break;
            case 2: emptyTitle.setText("Your reading list is empty");
                    emptySubtitle.setText("Save novels for later and find them here"); break;
            case 3: emptyTitle.setText("No favorites yet");
                    emptySubtitle.setText("Tap the heart on any novel to add it"); break;
            case 4: emptyTitle.setText("No downloads");
                    emptySubtitle.setText("Download novels to read offline"); break;
        }
    }
}
