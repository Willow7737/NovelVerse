package com.novelverse.app.presentation.profile.follows;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.repository.FollowsRepository;
import com.novelverse.app.domain.models.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for FollowsActivity.
 *
 * Scoped to the Activity so both tab Fragments share the same instance and
 * the Activity itself can observe counts without a second Supabase call.
 *
 * Key fixes vs. the previous stub:
 *  - Injects FollowsRepository instead of holding no dependencies.
 *  - loadAll() actually calls Supabase via the repository (no more hard-coded
 *    empty ArrayList).
 *  - Exposes followersCount / followingCount so the tab headers can show the
 *    live totals.
 *  - Loads are guarded: calling loadAll() a second time (e.g. config change)
 *    is a no-op if data is already present.
 */
@HiltViewModel
public class FollowsViewModel extends ViewModel {

    private final FollowsRepository repository;

    // ── Users lists ───────────────────────────────────────────────────────────
    private final MutableLiveData<List<User>> followers         = new MutableLiveData<>();
    private final MutableLiveData<List<User>> following         = new MutableLiveData<>();
    private final MutableLiveData<Boolean>    isLoadingFollowers = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>    isLoadingFollowing = new MutableLiveData<>(false);

    // ── Counts (used by tab headers) ──────────────────────────────────────────
    private final MutableLiveData<Integer>    followersCount    = new MutableLiveData<>(null);
    private final MutableLiveData<Integer>    followingCount    = new MutableLiveData<>(null);

    @Inject
    public FollowsViewModel(FollowsRepository repository) {
        this.repository = repository;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public LiveData<List<User>> getFollowers()          { return followers; }
    public LiveData<List<User>> getFollowing()          { return following; }
    public LiveData<Boolean>    getIsLoadingFollowers() { return isLoadingFollowers; }
    public LiveData<Boolean>    getIsLoadingFollowing() { return isLoadingFollowing; }
    public LiveData<Integer>    getFollowersCount()     { return followersCount; }
    public LiveData<Integer>    getFollowingCount()     { return followingCount; }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Load followers, following, and counts for the given user.
     * Safe to call multiple times — already-loaded data is not re-fetched.
     */
    public void loadAll(String userId) {
        if (userId == null || userId.isEmpty()) return;

        if (followers.getValue() == null)  fetchFollowers(userId);
        if (following.getValue() == null)  fetchFollowing(userId);
        if (followersCount.getValue() == null) fetchCounts(userId);
    }

    private void fetchFollowers(String userId) {
        isLoadingFollowers.postValue(true);
        repository.getFollows(userId, true, new FollowsRepository.FollowsCallback() {
            @Override
            public void onSuccess(List<User> users) {
                followers.postValue(users);
                isLoadingFollowers.postValue(false);
            }

            @Override
            public void onError(String error) {
                followers.postValue(new ArrayList<>());
                isLoadingFollowers.postValue(false);
            }
        });
    }

    private void fetchFollowing(String userId) {
        isLoadingFollowing.postValue(true);
        repository.getFollows(userId, false, new FollowsRepository.FollowsCallback() {
            @Override
            public void onSuccess(List<User> users) {
                following.postValue(users);
                isLoadingFollowing.postValue(false);
            }

            @Override
            public void onError(String error) {
                following.postValue(new ArrayList<>());
                isLoadingFollowing.postValue(false);
            }
        });
    }

    private void fetchCounts(String userId) {
        repository.getCounts(userId, new FollowsRepository.CountsCallback() {
            @Override
            public void onSuccess(int fc, int fg) {
                followersCount.postValue(fc);
                followingCount.postValue(fg);
            }

            @Override
            public void onError(String error) {
                // Leave the nulls — tabs will fall back to plain labels
            }
        });
    }

    // ── Optimistic toggle ─────────────────────────────────────────────────────

    /**
     * Optimistically update the isFollowing flag in the cached list so the
     * button state changes immediately without waiting for a server round-trip.
     *
     * @param targetUserId  The user whose follow state changed.
     * @param follow        The new desired state (true = follow, false = unfollow).
     * @param isFollowersList true if the user appears in the Followers tab list,
     *                        false for the Following tab list.
     *
     * TODO: Call a follow/unfollow API here and revert on failure.
     */
    public void toggleFollow(String targetUserId, boolean follow, boolean isFollowersList) {
        MutableLiveData<List<User>> target = isFollowersList ? followers : following;
        List<User> current = target.getValue();
        if (current == null) return;

        List<User> updated = new ArrayList<>(current.size());
        for (User u : current) {
            if (u.getId().equals(targetUserId)) u.setFollowing(follow);
            updated.add(u);
        }
        target.setValue(updated);
    }
}
