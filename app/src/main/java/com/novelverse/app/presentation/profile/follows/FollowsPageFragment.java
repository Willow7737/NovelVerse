package com.novelverse.app.presentation.profile.follows;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.common.views.DottedDividerDecoration;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FollowsPageFragment extends Fragment {

    private static final String ARG_USER_ID      = "user_id";
    private static final String ARG_IS_FOLLOWERS = "is_followers";

    private RecyclerView      recyclerView;
    private LinearLayout      emptyView;
    private TextView          emptyTitle;
    private TextView          emptySubtitle;
    private View              loadingView;
    private UserFollowAdapter adapter;
    private FollowsViewModel  viewModel;
    private boolean           isFollowers;

    public static FollowsPageFragment newInstance(String userId, boolean isFollowers) {
        FollowsPageFragment fragment = new FollowsPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putBoolean(ARG_IS_FOLLOWERS, isFollowers);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_follows_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = requireArguments().getString(ARG_USER_ID);
        isFollowers   = requireArguments().getBoolean(ARG_IS_FOLLOWERS);

        recyclerView  = view.findViewById(R.id.page_follows_rv);
        emptyView     = view.findViewById(R.id.page_follows_empty);
        emptyTitle    = view.findViewById(R.id.page_follows_empty_title);
        emptySubtitle = view.findViewById(R.id.page_follows_empty_subtitle);
        loadingView   = view.findViewById(R.id.page_follows_loading);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DottedDividerDecoration(requireContext()));
        recyclerView.setClipToPadding(false);

        adapter = new UserFollowAdapter();
        adapter.setOnItemClickListener(this::onUserClick);
        adapter.setOnFollowClickListener(this::onFollowClick);
        recyclerView.setAdapter(adapter);

        // Share the Activity-scoped ViewModel so both tabs and the Activity
        // observe the same data — avoids duplicate Supabase calls.
        viewModel = new ViewModelProvider(requireActivity()).get(FollowsViewModel.class);

        // Trigger the load (FollowsActivity also calls loadAll, but calling it
        // here is safe — ViewModel guards against duplicate fetches).
        viewModel.loadAll(userId);

        if (isFollowers) {
            viewModel.getIsLoadingFollowers().observe(getViewLifecycleOwner(), this::updateLoadingState);
            viewModel.getFollowers().observe(getViewLifecycleOwner(), this::updateUI);
        } else {
            viewModel.getIsLoadingFollowing().observe(getViewLifecycleOwner(), this::updateLoadingState);
            viewModel.getFollowing().observe(getViewLifecycleOwner(), this::updateUI);
        }
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    private void updateLoadingState(Boolean loading) {
        if (loadingView == null) return;
        loadingView.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
    }

    // ── List rendering ────────────────────────────────────────────────────────

    private void updateUI(List<User> users) {
        if (users == null || users.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyTitle.setText(isFollowers ? "No followers yet" : "Not following anyone");
            emptySubtitle.setText(isFollowers
                    ? "When people follow this account, they'll appear here."
                    : "When you follow people, they'll appear here.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.submitList(users);
        }
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    private void onUserClick(User user) {
        // TODO: Navigate to public profile (e.g. UserProfileActivity)
    }

    private void onFollowClick(User user, boolean follow) {
        viewModel.toggleFollow(user.getId(), follow, isFollowers);
    }
}
