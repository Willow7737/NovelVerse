package com.novelverse.app.presentation.search;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.repository.SearchRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.domain.models.TrendingItem;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

/** Tasks 18, 19, 20, 21: ViewModel for Search state preservation, debounce, filter, mode */
@HiltViewModel
public class SearchViewModel extends ViewModel {

    public enum SearchMode { NOVELS, AUTHORS, CHARACTERS }

    private final SearchRepository searchRepository;

    private final MutableLiveData<String>             currentQuery   = new MutableLiveData<>("");
    private final MutableLiveData<List<Novel>>        currentResults = new MutableLiveData<>();
    private final MutableLiveData<SearchFilterBottomSheet.SearchFilter> activeFilter = new MutableLiveData<>();
    private final MutableLiveData<SearchMode>         searchMode     = new MutableLiveData<>(SearchMode.NOVELS);
    private final MutableLiveData<List<TrendingItem>> trendingItems  = new MutableLiveData<>();
    private final MutableLiveData<Boolean>            isLoading      = new MutableLiveData<>(false);

    @Inject
    public SearchViewModel(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public LiveData<String>             getCurrentQuery()   { return currentQuery; }
    public LiveData<List<Novel>>        getCurrentResults() { return currentResults; }
    public LiveData<SearchFilterBottomSheet.SearchFilter> getActiveFilter() { return activeFilter; }
    public LiveData<SearchMode>         getSearchMode()     { return searchMode; }
    public LiveData<List<TrendingItem>> getTrendingItems()  { return trendingItems; }
    public LiveData<Boolean>            getIsLoading()      { return isLoading; }

    public void setSearchMode(SearchMode mode) { searchMode.setValue(mode); }

    public void search(String query) { search(query, activeFilter.getValue()); }

    public void search(String query, SearchFilterBottomSheet.SearchFilter filter) {
        currentQuery.setValue(query);
        activeFilter.setValue(filter);
        isLoading.setValue(true);
        searchRepository.search(query, filter, results -> {
            currentResults.postValue(results);
            isLoading.postValue(false);
        });
    }

    public void applyFilter(SearchFilterBottomSheet.SearchFilter filter) {
        String q = currentQuery.getValue();
        if (q != null && !q.isEmpty()) search(q, filter);
    }

    public void loadTrending() {
        searchRepository.getTrendingSearches(items -> trendingItems.postValue(items));
    }
}
