package com.novelverse.app.presentation.tts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.services.TtsService;
import com.novelverse.app.tts.TtsVoice;

/**
 * TtsMiniPlayerFragment
 *
 * A self-contained, reusable draggable mini-player pill for TTS playback.
 * Drop it into any Activity that needs to surface TTS controls while the
 * user is doing other things (reading, browsing home, etc.).
 *
 * ── Usage ────────────────────────────────────────────────────────────────
 *
 *   In your Activity layout, add a full-screen transparent container:
 *
 *     <FrameLayout
 *         android:id="@+id/tts_mini_player_container"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         android:background="@android:color/transparent"
 *         android:clickable="false"
 *         android:focusable="false" />
 *
 *   In your Activity or Fragment's onCreate / onViewCreated:
 *
 *     TtsMiniPlayerFragment miniPlayer = new TtsMiniPlayerFragment();
 *     getSupportFragmentManager().beginTransaction()
 *         .replace(R.id.tts_mini_player_container, miniPlayer, "tts_mini")
 *         .commitNow();
 *
 *   From a Fragment hosted in the same Activity, use the activity's
 *   FragmentManager so the overlay sits above everything:
 *
 *     TtsMiniPlayerFragment miniPlayer = new TtsMiniPlayerFragment();
 *     requireActivity().getSupportFragmentManager().beginTransaction()
 *         .replace(R.id.tts_mini_player_container, miniPlayer, "tts_mini")
 *         .commitNow();
 *
 * ── Lifecycle ────────────────────────────────────────────────────────────
 *   The Fragment binds to TtsService in onStart and unbinds in onStop.
 *   It registers itself as the ServiceListener while bound, which means
 *   TtsPlayerActivity (when open) will take over the listener and the
 *   Fragment will re-sync when the user returns.
 *
 * ── Visibility ───────────────────────────────────────────────────────────
 *   The pill shows/hides itself automatically in response to onPlaying,
 *   onPaused, onStopped, and onFinished callbacks. No manual show/hide
 *   calls are needed from the host.
 */
public class TtsMiniPlayerFragment extends Fragment implements TtsService.ServiceListener {

    // ── Views ─────────────────────────────────────────────────────────────
    private View      pill;
    private ImageView fabCover;
    private ImageView fabPlayPause;

    // ── Drag state ────────────────────────────────────────────────────────
    private float   touchOffsetX;
    private float   touchOffsetY;
    private float   lastRawX;
    private float   lastRawY;
    private boolean dragging = false;

    // ── Service binding ───────────────────────────────────────────────────
    private TtsService ttsService;
    private boolean    bound = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ttsService = ((TtsService.TtsBinder) binder).getService();
            ttsService.setServiceListener(TtsMiniPlayerFragment.this);
            bound = true;
            // Sync pill visibility and icon to whatever is already playing
            syncState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound      = false;
            ttsService = null;
        }
    };

    // ── Factory ───────────────────────────────────────────────────────────

    public static TtsMiniPlayerFragment newInstance() {
        return new TtsMiniPlayerFragment();
    }

    // ── Fragment lifecycle ────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root    = inflater.inflate(R.layout.fragment_tts_mini_player, container, false);
        pill         = root.findViewById(R.id.tts_mini_fab);
        fabCover     = root.findViewById(R.id.fab_cover);
        fabPlayPause = root.findViewById(R.id.fab_play_pause);
        View fabClose = root.findViewById(R.id.fab_close);

        wireListeners(fabClose);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind with BIND_AUTO_CREATE so we get notified even if service just started
        Intent intent = new Intent(requireContext(), TtsService.class);
        requireContext().bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (bound) {
            if (ttsService != null) ttsService.setServiceListener(null);
            requireContext().unbindService(conn);
            bound      = false;
            ttsService = null;
        }
    }

    // ── View wiring ───────────────────────────────────────────────────────

    private void wireListeners(View fabClose) {

        // Drag + tap on the pill body
        pill.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    touchOffsetX = event.getRawX() - v.getX();
                    touchOffsetY = event.getRawY() - v.getY();
                    lastRawX     = event.getRawX();
                    lastRawY     = event.getRawY();
                    dragging     = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getRawX() - lastRawX) > 8 ||
                        Math.abs(event.getRawY() - lastRawY) > 8) {
                        dragging = true;
                    }
                    if (dragging) {
                        v.setX(event.getRawX() - touchOffsetX);
                        v.setY(event.getRawY() - touchOffsetY);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!dragging) openFullPlayer();
                    return true;
            }
            return false;
        });

        // Play / Pause
        if (fabPlayPause != null) {
            fabPlayPause.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendCommand(TtsService.ACTION_PLAY_PAUSE);
            });
        }

        // Close / Stop
        if (fabClose != null) {
            fabClose.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendCommand(TtsService.ACTION_STOP);
                hidePill();
            });
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /**
     * Called right after binding. Reflects the current service state into
     * the pill so the UI is correct even if playback started before this
     * Fragment was attached (e.g. user navigates to HomeFragment mid-session).
     */
    private void syncState() {
        if (ttsService == null || !isAdded()) return;
        boolean active = ttsService.isPlaying() || ttsService.isPaused();
        if (active) {
            refreshCover();
            setPlayIcon(ttsService.isPlaying());
            showPill();
        } else {
            hidePill();
        }
    }

    private void setPlayIcon(boolean playing) {
        if (fabPlayPause == null) return;
        fabPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void refreshCover() {
        if (!isAdded() || fabCover == null || ttsService == null) return;
        String url = ttsService.getCoverUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                 .load(url)
                 .centerCrop()
                 .placeholder(R.drawable.img_cover_placeholder_default)
                 .into(fabCover);
        }
    }

    private void openFullPlayer() {
        if (ttsService == null) return;
        Intent intent = new Intent(requireContext(), TtsPlayerActivity.class);
        intent.putExtra(TtsPlayerActivity.EXTRA_NOVEL_TITLE,   ttsService.getNovelTitle());
        intent.putExtra(TtsPlayerActivity.EXTRA_CHAPTER_TITLE, ttsService.getChapterTitle());
        intent.putExtra(TtsPlayerActivity.EXTRA_NOVEL_ID,      ttsService.getNovelId());
        intent.putExtra(TtsPlayerActivity.EXTRA_CHAPTER_ID,    ttsService.getChapterId());
        intent.putExtra(TtsPlayerActivity.EXTRA_COVER_URL,     ttsService.getCoverUrl());
        // EXTRA_CHAPTER_TEXT is intentionally omitted: TtsPlayerActivity re-uses the
        // already-running service state. The text is already chunked in TtsService.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void sendCommand(String action) {
        if (!isAdded()) return;
        try {
            Intent cmd = new Intent(requireContext(), TtsService.class);
            cmd.setAction(action);
            requireContext().startService(cmd);
        } catch (Exception ignored) {}
    }

    // ── Animated show / hide ──────────────────────────────────────────────

    public void showPill() {
        if (pill == null) return;
        pill.setVisibility(View.VISIBLE);
        pill.setAlpha(0f);
        pill.setScaleX(0.7f);
        pill.setScaleY(0.7f);
        pill.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(260)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }

    public void hidePill() {
        if (pill == null || pill.getVisibility() != View.VISIBLE) return;
        pill.animate()
            .alpha(0f).scaleX(0.7f).scaleY(0.7f)
            .setDuration(200)
            .withEndAction(() -> pill.setVisibility(View.GONE))
            .start();
    }

    // ── TtsService.ServiceListener ────────────────────────────────────────

    @Override public void onChaptersLoaded(int totalChunks) {}
    @Override public void onChunkStarted(int chunkIndex, String chunkText) {}
    @Override public void onProgress(int chunkIndex, int totalChunks) {}
    @Override public void onVoiceChanged(TtsVoice voice) {}
    @Override public void onSpeedChanged(float speed) {}
    @Override public void onModeChanged(String activeMode) {}

    @Override
    public void onPlaying(int chunkIndex) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            refreshCover();
            setPlayIcon(true);
            showPill();
        });
    }

    @Override
    public void onPaused(int chunkIndex) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> setPlayIcon(false));
    }

    @Override
    public void onStopped() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::hidePill);
    }

    @Override
    public void onFinished() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::hidePill);
    }
}
