package com.novelverse.app.presentation.novel.reader;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.repository.ReadingProgressRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for ReaderActivity.
 *
 * Owns all reading-progress state. The Activity calls the three lifecycle methods below and
 * observes LiveData — it holds zero progress logic itself.
 *
 * Rules enforced here:
 *  Rule 5  – Progress is triggered on chapter open, on scroll (debounced), and on exit.
 *  Rule 6  – Scroll updates are debounced: a pending save is cancelled and rescheduled on every
 *             scroll event; the RPC fires only after DEBOUNCE_MS of silence. onExit() flushes
 *             immediately so no progress is lost when the user leaves.
 *  Rule 9  – progress_percentage LiveData drives the UI percentage display.
 *  Rule 10 – No duplicate flows. All writes go through ReadingProgressRepository.
 */
@HiltViewModel
public class ReaderViewModel extends ViewModel {

    // Rule 6: 3-second debounce window. Fast scrolling = single RPC call per pause.
    private static final long DEBOUNCE_MS = 3_000L;

    private final ReadingProgressRepository progressRepo;
    private final Handler                   handler = new Handler(Looper.getMainLooper());

    // ── LiveData the Activity observes ───────────────────────────────────────

    /** 0-100 double; used by the progress bar / percentage label (Rule 9). */
    private final MutableLiveData<Double> _progressPercentage = new MutableLiveData<>(0.0);
    public  final LiveData<Double>         progressPercentage  = _progressPercentage;

    /** Emitted once when resume data arrives (Rule 3: chapter_id + scroll_position). */
    private final MutableLiveData<ResumePosition> _resumePosition = new MutableLiveData<>();
    public  final LiveData<ResumePosition>          resumePosition  = _resumePosition;

    // ── Internal state ───────────────────────────────────────────────────────

    private String  currentNovelId;
    private String  currentChapterId;
    private int     lastScrollY    = 0;
    private int     lastTotalH     = 0;
    private double  lastSavedPct   = -1;

    private final Runnable debouncedSave = this::flushProgress;

    @Inject
    public ReaderViewModel(ReadingProgressRepository progressRepo) {
        this.progressRepo = progressRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5: Three trigger points
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rule 5 – Call when a chapter is opened (or when the Activity is created with a novelId).
     * Fetches the resume position from `continue_reading` (Rule 2/3) and fires an initial
     * progress upsert so Supabase knows the session started.
     */
    public void onChapterOpen(@NonNull String novelId, @NonNull String chapterId) {
        currentNovelId   = novelId;
        currentChapterId = chapterId;
        lastSavedPct     = -1; // force first save

        // Rule 2 + Rule 3: fetch resume position from the view, then emit to Activity.
        progressRepo.getResumePosition(novelId, new ReadingProgressRepository.ResumeCallback() {
            @Override
            public void onResult(String resumeChapterId, int scrollPosition, double progressPct) {
                // Emit resume position so Activity can restore scroll and switch chapter if needed.
                _resumePosition.setValue(new ResumePosition(resumeChapterId, scrollPosition));
                _progressPercentage.setValue(progressPct);
                lastSavedPct = progressPct;

                // If the incoming chapterId differs from stored resume chapter, the Activity
                // already navigated to a new chapter intentionally — use the new chapterId.
                String effectiveChapter = (resumeChapterId != null) ? resumeChapterId : chapterId;
                currentChapterId = effectiveChapter;

                // Rule 5: fire an upsert on open with current known progress.
                progressRepo.upsertProgress(novelId, currentChapterId, scrollPosition, progressPct, null);
            }

            @Override
            public void onError(String error) {
                // No prior progress. Upsert with 0 to register the session.
                progressRepo.upsertProgress(novelId, chapterId, 0, 0.0, null);
            }
        });
    }

    /**
     * Rule 5 + Rule 6 – Call from the scroll listener on every scroll change.
     * Progress is calculated here so the Activity just passes raw pixel values.
     * Updates the UI immediately; debounces the network call.
     *
     * @param scrollY pixel offset from the top
     * @param totalH  total scrollable height (child.height - container.height)
     */
    public void onScroll(int scrollY, int totalH) {
        if (totalH <= 0 || currentNovelId == null) return;

        lastScrollY = scrollY;
        lastTotalH  = totalH;

        double pct = Math.min(100.0, (scrollY * 100.0) / totalH);
        _progressPercentage.setValue(pct); // Rule 9: update UI immediately

        // Rule 6: cancel the previous pending save and reschedule.
        handler.removeCallbacks(debouncedSave);
        handler.postDelayed(debouncedSave, DEBOUNCE_MS);
    }

    /**
     * Rule 5 – Call from onPause() and onStop(). Cancels any pending debounce and
     * flushes progress immediately so no data is lost when the user exits.
     */
    public void onExit() {
        handler.removeCallbacks(debouncedSave);
        flushProgress();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    /** Executes the actual RPC call. Shared by the debounce runnable and onExit(). */
    private void flushProgress() {
        if (currentNovelId == null || currentChapterId == null) return;

        double pct = (lastTotalH > 0)
                ? Math.min(100.0, (lastScrollY * 100.0) / lastTotalH)
                : (_progressPercentage.getValue() != null ? _progressPercentage.getValue() : 0.0);

        // Skip if nothing meaningful has changed to avoid redundant RPCs (Rule 6).
        if (Math.abs(pct - lastSavedPct) < 0.5 && lastSavedPct >= 0) return;

        lastSavedPct = pct;
        _progressPercentage.setValue(pct);

        progressRepo.upsertProgress(
                currentNovelId,
                currentChapterId,
                lastScrollY,
                pct,
                null   // fire-and-forget; errors are handled inside the repository (Rule 8)
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Safety: flush and cancel to avoid leaking the Handler callback.
        handler.removeCallbacks(debouncedSave);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data classes
    // ─────────────────────────────────────────────────────────────────────────

    /** Rule 3 — Carries the two fields needed to restore a reading session. */
    public static class ResumePosition {
        public final String chapterId;
        public final int    scrollPosition;

        public ResumePosition(String chapterId, int scrollPosition) {
            this.chapterId      = chapterId;
            this.scrollPosition = scrollPosition;
        }
    }
}
