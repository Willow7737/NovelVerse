package com.novelverse.app.presentation.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.ReadingChallenge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final NovelRepository novelRepository;
    private final UserPreferences userPreferences;

    private final MutableLiveData<List<Novel>> featuredNovels = new MutableLiveData<>();
    private final MutableLiveData<List<Novel>> trendingNovels = new MutableLiveData<>();
    private final MutableLiveData<List<Novel>> newReleases = new MutableLiveData<>();
    private final MutableLiveData<List<String>> genres = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Novel>> continueReadingNovels = new MutableLiveData<>();
    private final MutableLiveData<List<Novel>> forYouNovels = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingChallenge>> activeChallenges =
            new MutableLiveData<>();

    private List<Novel> allFeatured = new ArrayList<>();
    private List<Novel> allTrending = new ArrayList<>();
    private List<Novel> allNewReleases = new ArrayList<>();

    // Keep hard references to LiveData + Observers so they are not GC'd before
    // the one-shot callback fires (fixes the "featured section empty" bug where
    // observeForever on a locally-scoped LiveData was being GC'd before emit).
    private LiveData<List<Novel>> featuredSource;
    private LiveData<List<Novel>> trendingSource;
    private LiveData<List<Novel>> newReleasesSource;
    private LiveData<List<Novel>> continueSource;
    private LiveData<List<Novel>> forYouSource;
    private LiveData<List<ReadingChallenge>> challengesSource;

    private Observer<List<Novel>> featuredObserver;
    private Observer<List<Novel>> trendingObserver;
    private Observer<List<Novel>> newReleasesObserver;
    private Observer<List<Novel>> continueObserver;
    private Observer<List<Novel>> forYouObserver;
    private Observer<List<ReadingChallenge>> challengesObserver;

    @Inject
    public HomeViewModel(NovelRepository novelRepository, UserPreferences userPreferences) {
        this.novelRepository = novelRepository;
        this.userPreferences = userPreferences;
    }

    public LiveData<List<Novel>> getFeaturedNovels() {
        return featuredNovels;
    }

    public LiveData<List<Novel>> getTrendingNovels() {
        return trendingNovels;
    }

    public LiveData<List<Novel>> getNewReleases() {
        return newReleases;
    }

    public LiveData<List<String>> getGenres() {
        return genres;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<Novel>> getContinueReadingNovels() {
        return continueReadingNovels;
    }

    public LiveData<List<Novel>> getForYouNovels() {
        return forYouNovels;
    }

    public LiveData<List<ReadingChallenge>> getActiveChallenges() {
        return activeChallenges;
    }

    // ── Genre filter ──────────────────────────────────────────────────────

    public void filterByGenre(String genre) {
        boolean all = (genre == null || genre.isEmpty() || "All".equalsIgnoreCase(genre));
        featuredNovels.postValue(all ? allFeatured : filterList(allFeatured, genre));
        trendingNovels.postValue(all ? allTrending : filterList(allTrending, genre));
        newReleases.postValue(all ? allNewReleases : filterList(allNewReleases, genre));
    }

    private List<Novel> filterList(List<Novel> source, String genre) {
        List<Novel> result = new ArrayList<>();
        if (source == null) return result;
        for (Novel n : source) {
            if (n.getGenres() != null) {
                for (String g : n.getGenres()) {
                    if (g.equalsIgnoreCase(genre)) {
                        result.add(n);
                        break;
                    }
                }
            }
        }
        return result;
    }

    // ── Load methods (fixed: hold strong ref to source + observer) ────────

    public void loadFeaturedNovels() {
        isLoading.postValue(true);
        // Remove previous observer to avoid duplicate callbacks
        if (featuredSource != null && featuredObserver != null) {
            featuredSource.removeObserver(featuredObserver);
        }
        featuredSource = novelRepository.getFeaturedNovels();
        featuredObserver =
                novels -> {
                    allFeatured = novels != null ? novels : new ArrayList<>();
                    featuredNovels.postValue(allFeatured);
                    isLoading.postValue(false);
                };
        featuredSource.observeForever(featuredObserver);
    }

    public void loadTrendingNovels() {
        if (trendingSource != null && trendingObserver != null) {
            trendingSource.removeObserver(trendingObserver);
        }
        trendingSource = novelRepository.getTrendingNovels();
        trendingObserver =
                novels -> {
                    allTrending = novels != null ? novels : new ArrayList<>();
                    trendingNovels.postValue(allTrending);
                };
        trendingSource.observeForever(trendingObserver);
    }

    public void loadNewReleases() {
        if (newReleasesSource != null && newReleasesObserver != null) {
            newReleasesSource.removeObserver(newReleasesObserver);
        }
        newReleasesSource = novelRepository.getNewReleases();
        newReleasesObserver =
                novels -> {
                    allNewReleases = novels != null ? novels : new ArrayList<>();
                    newReleases.postValue(allNewReleases);
                };
        newReleasesSource.observeForever(newReleasesObserver);
    }

    public void loadGenres() {
        genres.postValue(
                Arrays.asList(
                        "Fantasy",
                        "Romance",
                        "Sci-Fi",
                        "Mystery",
                        "Thriller",
                        "Horror",
                        "Adventure",
                        "Comedy",
                        "Drama"));
    }

    public void loadContinueReading() {
        if (continueSource != null && continueObserver != null) {
            continueSource.removeObserver(continueObserver);
        }
        continueSource = novelRepository.getContinueReadingNovels();
        continueObserver =
                novels ->
                        continueReadingNovels.postValue(
                                novels != null ? novels : new ArrayList<>());
        continueSource.observeForever(continueObserver);
    }

    public void loadForYou() {
        List<String> preferredGenres = userPreferences.getPreferredGenres();
        if (forYouSource != null && forYouObserver != null) {
            forYouSource.removeObserver(forYouObserver);
        }
        forYouSource = novelRepository.getNovelsByGenres(preferredGenres);
        forYouObserver =
                novels -> forYouNovels.postValue(novels != null ? novels : new ArrayList<>());
        forYouSource.observeForever(forYouObserver);
    }

    public void loadActiveChallenges() {
        if (challengesSource != null && challengesObserver != null) {
            challengesSource.removeObserver(challengesObserver);
        }
        challengesSource = novelRepository.getActiveChallenges();
        challengesObserver =
                challenges ->
                        activeChallenges.postValue(
                                challenges != null ? challenges : new ArrayList<>());
        challengesSource.observeForever(challengesObserver);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        if (featuredSource != null && featuredObserver != null)
            featuredSource.removeObserver(featuredObserver);
        if (trendingSource != null && trendingObserver != null)
            trendingSource.removeObserver(trendingObserver);
        if (newReleasesSource != null && newReleasesObserver != null)
            newReleasesSource.removeObserver(newReleasesObserver);
        if (continueSource != null && continueObserver != null)
            continueSource.removeObserver(continueObserver);
        if (forYouSource != null && forYouObserver != null)
            forYouSource.removeObserver(forYouObserver);
        if (challengesSource != null && challengesObserver != null)
            challengesSource.removeObserver(challengesObserver);
    }
}
