package com.novelverse.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.novelverse.app.R;
import com.novelverse.app.tts.TtsNetworkMonitor;
import com.novelverse.app.tts.TtsPreferences;
import com.novelverse.app.tts.TtsVoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hybrid Text-to-Speech foreground service.
 *
 * Routing priority:
 *   1. If mode is OFFLINE (or network is down): Android system TTS
 *   2. If mode is AUTO or ONLINE and network is up:
 *        a. Piper voice -> Oracle Cloud :5000  (fast, <300ms latency)
 *        b. Coqui voice -> Oracle Cloud :5002  (high quality, ~800ms)
 *   3. If remote synthesis fails: silent fall back to Android system TTS
 *
 * NOTE: Remote endpoints are STUBBED until Oracle is live.
 * synthesizeRemote() currently calls fallbackToSystemTts().
 * When ready: replace the two TODO blocks in that method with actual OkHttp calls.
 */
public class TtsService extends Service implements TextToSpeech.OnInitListener,
        TtsNetworkMonitor.Listener {

    private static final String TAG = "TtsService";

    // Notification
    private static final String CHANNEL_ID    = "tts_channel";
    private static final int    NOTIFICATION_ID = 1001;

    public static final String ACTION_PLAY_PAUSE = "com.novelverse.app.TTS_PLAY_PAUSE";
    public static final String ACTION_STOP        = "com.novelverse.app.TTS_STOP";
    public static final String ACTION_PREV_CHUNK  = "com.novelverse.app.TTS_PREV";
    public static final String ACTION_NEXT_CHUNK  = "com.novelverse.app.TTS_NEXT";

    public enum PlaybackState { IDLE, PLAYING, PAUSED, STOPPED }

    private PlaybackState      playbackState     = PlaybackState.IDLE;
    private TtsVoice           currentVoice;
    private List<String>       chunks            = new ArrayList<>();
    private int                currentChunkIndex = 0;
    private String             novelTitle        = "";
    private String             chapterTitle      = "";

    private TextToSpeech       systemTts;
    private boolean            systemTtsReady    = false;

    private TtsPreferences     ttsPrefs;
    private TtsNetworkMonitor  networkMonitor;
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private final Handler      mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean speaking   = new AtomicBoolean(false);

    private final IBinder binder = new TtsBinder();
    private ServiceListener serviceListener;

    public class TtsBinder extends Binder {
        public TtsService getService() { return TtsService.this; }
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        ttsPrefs       = new TtsPreferences(this);
        networkMonitor = new TtsNetworkMonitor(this);
        networkMonitor.addListener(this);
        networkMonitor.start();
        currentVoice   = ttsPrefs.getVoice();
        systemTts      = new TextToSpeech(this, this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleNotificationAction(intent.getAction());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        networkMonitor.stop();
        stopPlayback();
        if (systemTts != null) { systemTts.stop(); systemTts.shutdown(); }
        executor.shutdownNow();
        super.onDestroy();
    }

    // ── TextToSpeech.OnInitListener ───────────────────────────────────────

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = systemTts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                systemTts.setLanguage(Locale.US);
            }
            systemTts.setSpeechRate(ttsPrefs.getSpeed());
            systemTts.setPitch(ttsPrefs.getPitch());
            systemTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {
                    mainHandler.post(TtsService.this::notifyPlaybackStarted);
                }
                @Override public void onDone(String id) {
                    speaking.set(false);
                    mainHandler.post(TtsService.this::onChunkFinished);
                }
                @Override public void onError(String id) {
                    speaking.set(false);
                    Log.e(TAG, "System TTS error for: " + id);
                    mainHandler.post(TtsService.this::onChunkError);
                }
            });
            systemTtsReady = true;
        } else {
            Log.e(TAG, "System TTS init failed: " + status);
        }
    }

    // ── TtsNetworkMonitor.Listener ────────────────────────────────────────

    @Override
    public void onNetworkChanged(boolean isOnline) {
        Log.d(TAG, "Network: " + (isOnline ? "ONLINE" : "OFFLINE"));
        if (serviceListener != null) serviceListener.onModeChanged(resolveActiveMode());
        updateNotification();
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Load chapter text and break it into synthesis chunks. Call before play(). */
    public void loadChapter(String text, String novelTitle, String chapterTitle) {
        stopPlayback();
        this.novelTitle   = novelTitle   != null ? novelTitle   : "";
        this.chapterTitle = chapterTitle != null ? chapterTitle : "";
        this.chunks        = chunkText(text);
        this.currentChunkIndex = 0;
        this.playbackState = PlaybackState.IDLE;
        if (serviceListener != null) serviceListener.onChaptersLoaded(chunks.size());
    }

    public void play() {
        if (chunks.isEmpty()) return;
        if (playbackState == PlaybackState.PAUSED) {
            playbackState = PlaybackState.PLAYING;
            speakChunk(currentChunkIndex);
        } else {
            playbackState = PlaybackState.PLAYING;
            currentChunkIndex = 0;
            speakChunk(0);
        }
    }

    public void pause() {
        if (playbackState != PlaybackState.PLAYING) return;
        playbackState = PlaybackState.PAUSED;
        speaking.set(false);
        if (systemTtsReady) systemTts.stop();
        updateNotification();
        if (serviceListener != null) serviceListener.onPaused(currentChunkIndex);
    }

    public void togglePlayPause() {
        if (playbackState == PlaybackState.PLAYING) pause(); else play();
    }

    public void stopPlayback() {
        playbackState = PlaybackState.STOPPED;
        speaking.set(false);
        if (systemTtsReady) systemTts.stop();
        currentChunkIndex = 0;
        updateNotification();
        if (serviceListener != null) serviceListener.onStopped();
    }

    public void nextChunk() {
        if (currentChunkIndex < chunks.size() - 1) {
            if (systemTtsReady) systemTts.stop();
            speaking.set(false);
            currentChunkIndex++;
            if (playbackState == PlaybackState.PLAYING) speakChunk(currentChunkIndex);
        }
    }

    public void prevChunk() {
        if (currentChunkIndex > 0) {
            if (systemTtsReady) systemTts.stop();
            speaking.set(false);
            currentChunkIndex--;
            if (playbackState == PlaybackState.PLAYING) speakChunk(currentChunkIndex);
        }
    }

    public void seekToChunk(int index) {
        if (index < 0 || index >= chunks.size()) return;
        if (systemTtsReady) systemTts.stop();
        speaking.set(false);
        currentChunkIndex = index;
        if (playbackState == PlaybackState.PLAYING) speakChunk(index);
        if (serviceListener != null) serviceListener.onProgress(index, chunks.size());
    }

    public void setVoice(TtsVoice voice) {
        boolean wasPlaying = (playbackState == PlaybackState.PLAYING);
        if (wasPlaying) pause();
        currentVoice = voice;
        ttsPrefs.setVoiceId(voice.getId());
        if (wasPlaying) play();
        if (serviceListener != null) serviceListener.onVoiceChanged(voice);
    }

    /** Set speed (0.5 - 3.0) */
    public void setSpeed(float speed) {
        ttsPrefs.setSpeed(speed);
        if (systemTtsReady) systemTts.setSpeechRate(speed);
        if (serviceListener != null) serviceListener.onSpeedChanged(speed);
    }

    public void setMode(String mode) {
        ttsPrefs.setMode(mode);
        if (serviceListener != null) serviceListener.onModeChanged(resolveActiveMode());
    }

    public void setServiceListener(ServiceListener listener) {
        this.serviceListener = listener;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public PlaybackState getPlaybackState()  { return playbackState; }
    public boolean isPlaying()               { return playbackState == PlaybackState.PLAYING; }
    public boolean isPaused()                { return playbackState == PlaybackState.PAUSED; }
    public float   getSpeed()                { return ttsPrefs.getSpeed(); }
    public TtsVoice getCurrentVoice()        { return currentVoice; }
    public int     getCurrentChunkIndex()    { return currentChunkIndex; }
    public int     getTotalChunks()          { return chunks.size(); }
    public java.util.List<String> getChunks() { return java.util.Collections.unmodifiableList(chunks); }
    public String  getNovelTitle()           { return novelTitle; }
    public String  getChapterTitle()         { return chapterTitle; }
    public boolean isInOnlineMode()          { return "online".equals(resolveActiveMode()); }
    public boolean isInOfflineMode()         { return "offline".equals(resolveActiveMode()); }

    public String resolveActiveMode() {
        String prefMode = ttsPrefs.getMode();
        if ("offline".equals(prefMode)) return "offline";
        if ("online".equals(prefMode))  return networkMonitor.isOnline() ? "online" : "offline";
        return networkMonitor.isOnline() ? "online" : "offline"; // auto
    }

    // ── Synthesis routing ─────────────────────────────────────────────────

    private void speakChunk(int index) {
        if (index >= chunks.size()) { onAllChunksDone(); return; }
        String text = chunks.get(index);
        if (text.trim().isEmpty()) { onChunkFinished(); return; }
        if (serviceListener != null) {
            serviceListener.onProgress(index, chunks.size());
            serviceListener.onChunkStarted(index, text);
        }
        updateNotification();
        if ("online".equals(resolveActiveMode())
                && !currentVoice.isSystemVoice()
                && networkMonitor.isOnline()) {
            synthesizeRemote(text, index);
        } else {
            synthesizeWithSystemTts(text);
        }
    }

    /**
     * Remote synthesis via Oracle Cloud (STUBBED until endpoint is live).
     *
     * When Oracle is ready, replace each TODO block with real OkHttp calls.
     * Only these two blocks need to change — everything else stays as-is.
     */
    private void synthesizeRemote(String text, int chunkIndex) {
        executor.execute(() -> {
            try {
                if (TtsVoice.ENGINE_PIPER.equals(currentVoice.getEngine())) {
                    // TODO (Piper): POST to https://YOUR_ORACLE_IP/piper/synthesize
                    // Body: { "text": text, "voice": currentVoice.getModelId(),
                    //         "length_scale": 1.0f / ttsPrefs.getSpeed() }
                    // On success: call playAudioBytes(response.body().bytes())
                    mainHandler.post(() -> fallbackToSystemTts(text));

                } else if (TtsVoice.ENGINE_COQUI.equals(currentVoice.getEngine())) {
                    // TODO (Coqui): POST to https://YOUR_ORACLE_IP/coqui/api/tts
                    // Body: { "text": text, "model_name": currentVoice.getModelId(),
                    //         "speaker_id": currentVoice.getSpeakerId(),  // nullable
                    //         "speed": ttsPrefs.getSpeed() }
                    // On success: call playAudioBytes(response.body().bytes())
                    mainHandler.post(() -> fallbackToSystemTts(text));

                } else {
                    mainHandler.post(() -> fallbackToSystemTts(text));
                }
            } catch (Exception e) {
                Log.e(TAG, "Remote synthesis failed: " + e.getMessage());
                mainHandler.post(() -> fallbackToSystemTts(text));
            }
        });
    }

    /** Plays raw WAV/MP3 bytes from remote server. Wired but not yet called. */
    @SuppressWarnings("unused")
    private void playAudioBytes(byte[] audioBytes) throws Exception {
        java.io.File tmpFile = java.io.File.createTempFile("tts_chunk", ".wav", getCacheDir());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
            fos.write(audioBytes);
        }
        mainHandler.post(() -> {
            android.media.MediaPlayer mp = new android.media.MediaPlayer();
            try {
                mp.setDataSource(tmpFile.getAbsolutePath());
                mp.prepareAsync();
                mp.setOnPreparedListener(p -> { speaking.set(true); p.start(); });
                mp.setOnCompletionListener(p -> {
                    speaking.set(false); p.release(); tmpFile.delete(); onChunkFinished();
                });
                mp.setOnErrorListener((p, w, x) -> {
                    speaking.set(false); p.release(); tmpFile.delete(); onChunkError(); return true;
                });
            } catch (Exception e) {
                mp.release(); tmpFile.delete();
                fallbackToSystemTts(chunks.get(currentChunkIndex));
            }
        });
    }

    private void synthesizeWithSystemTts(String text) {
        if (!systemTtsReady) { Log.e(TAG, "System TTS not ready"); return; }
        speaking.set(true);
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "chunk_" + currentChunkIndex);
        systemTts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void fallbackToSystemTts(String text) {
        Log.d(TAG, "Falling back to system TTS for chunk " + currentChunkIndex);
        synthesizeWithSystemTts(text);
    }

    private void notifyPlaybackStarted() {
        startForeground(NOTIFICATION_ID, buildNotification());
        if (serviceListener != null) serviceListener.onPlaying(currentChunkIndex);
    }

    private void onChunkFinished() {
        if (playbackState != PlaybackState.PLAYING) return;
        int next = currentChunkIndex + 1;
        if (next < chunks.size()) { currentChunkIndex = next; speakChunk(next); }
        else onAllChunksDone();
    }

    private void onChunkError() {
        Log.w(TAG, "Chunk error — retrying with system TTS");
        if (!chunks.isEmpty()) fallbackToSystemTts(chunks.get(currentChunkIndex));
    }

    private void onAllChunksDone() {
        playbackState = PlaybackState.IDLE;
        speaking.set(false);
        stopForeground(true);
        if (serviceListener != null) serviceListener.onFinished();
    }

    // ── Text chunking ─────────────────────────────────────────────────────

    private List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) return new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> result = new ArrayList<>();
        int targetWords = ttsPrefs.getChunkSizeWords();
        StringBuilder current = new StringBuilder();
        int wordCount = 0;
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                if (ttsPrefs.isSkipEmptyParagraphsEnabled()) continue;
                trimmed = "...";
            }
            int paraWords = trimmed.split("\\s+").length;
            if (wordCount > 0 && wordCount + paraWords > targetWords) {
                result.add(current.toString().trim());
                current.setLength(0); wordCount = 0;
            }
            if (current.length() > 0) current.append("\n\n");
            current.append(trimmed);
            wordCount += paraWords;
        }
        if (current.length() > 0) result.add(current.toString().trim());
        if (result.isEmpty() && !text.trim().isEmpty()) {
            String[] words = text.trim().split("\\s+");
            StringBuilder sb = new StringBuilder(); int wc = 0;
            for (String w : words) {
                sb.append(w).append(' '); wc++;
                if (wc >= targetWords) {
                    result.add(sb.toString().trim()); sb.setLength(0); wc = 0;
                }
            }
            if (sb.length() > 0) result.add(sb.toString().trim());
        }
        return result;
    }

    // ── Notification ──────────────────────────────────────────────────────

    private void handleNotificationAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE: togglePlayPause(); break;
            case ACTION_STOP:       stopPlayback();    break;
            case ACTION_PREV_CHUNK: prevChunk();       break;
            case ACTION_NEXT_CHUNK: nextChunk();       break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Read Aloud", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Controls for text-to-speech narration");
            ch.setShowBadge(false);
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this,
            com.novelverse.app.presentation.novel.reader.TtsPlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent prevPi  = buildActionIntent(ACTION_PREV_CHUNK, 1);
        PendingIntent pausePi = buildActionIntent(ACTION_PLAY_PAUSE, 2);
        PendingIntent nextPi  = buildActionIntent(ACTION_NEXT_CHUNK, 3);
        PendingIntent stopPi  = buildActionIntent(ACTION_STOP,       4);
        String contentText = (chapterTitle.isEmpty() ? "" : chapterTitle + " · ")
            + "Chunk " + (currentChunkIndex + 1) + " of " + chunks.size();
        boolean playing = (playbackState == PlaybackState.PLAYING);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(novelTitle.isEmpty() ? "Reading Aloud" : novelTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_play_circle)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_arrow_left, "Prev", prevPi)
            .addAction(playing ? R.drawable.ic_pause : R.drawable.ic_play,
                       playing ? "Pause" : "Play", pausePi)
            .addAction(R.drawable.ic_arrow_right, "Next", nextPi)
            .addAction(R.drawable.ic_close, "Stop", stopPi)
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private PendingIntent buildActionIntent(String action, int requestCode) {
        Intent i = new Intent(this, TtsService.class);
        i.setAction(action);
        return PendingIntent.getService(this, requestCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        if (playbackState == PlaybackState.IDLE || playbackState == PlaybackState.STOPPED) return;
        NotificationManager mgr = getSystemService(NotificationManager.class);
        if (mgr != null) mgr.notify(NOTIFICATION_ID, buildNotification());
    }

    // ── ServiceListener interface ─────────────────────────────────────────

    public interface ServiceListener {
        void onChaptersLoaded(int totalChunks);
        void onChunkStarted(int chunkIndex, String chunkText);
        void onPlaying(int chunkIndex);
        void onPaused(int chunkIndex);
        void onStopped();
        void onFinished();
        void onProgress(int chunkIndex, int totalChunks);
        void onVoiceChanged(TtsVoice voice);
        void onSpeedChanged(float speed);
        void onModeChanged(String activeMode);
    }
}
