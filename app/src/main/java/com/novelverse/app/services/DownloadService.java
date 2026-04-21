package com.novelverse.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.novelverse.app.NovelVerseApplication;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.Chapter;
import com.novelverse.app.domain.models.Novel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for downloading novels/chapters for offline reading
 */
public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFICATION_ID = 1002;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private DownloadListener downloadListener;

    private final IBinder binder = new DownloadBinder();

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Download a novel
     */
    public void downloadNovel(Novel novel) {
        startForeground(NOTIFICATION_ID, buildNotification("Downloading " + novel.getTitle(), 0));
        
        executor.execute(() -> {
            try {
                // Download novel metadata
                updateNotification("Downloading " + novel.getTitle(), 10);
                
                // Download chapters
                // for each chapter: download content
                
                updateNotification("Download complete", 100);
                
                if (downloadListener != null) {
                    downloadListener.onNovelDownloaded(novel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                if (downloadListener != null) {
                    downloadListener.onDownloadError(novel.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * Download a chapter
     */
    public void downloadChapter(Chapter chapter) {
        executor.execute(() -> {
            try {
                // Download chapter content
                
                if (downloadListener != null) {
                    downloadListener.onChapterDownloaded(chapter);
                }
            } catch (Exception e) {
                Log.e(TAG, "Chapter download failed", e);
                if (downloadListener != null) {
                    downloadListener.onDownloadError(chapter.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * Cancel download
     */
    public void cancelDownload(String novelId) {
        // Cancel ongoing download
    }

    /**
     * Set download listener
     */
    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    /**
     * Create notification channel
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Novel download progress");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Build notification
     */
    private Notification buildNotification(String content, int progress) {
        Intent intent = new Intent(this, com.novelverse.app.presentation.library.LibraryFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NovelVerse Download")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0);

        return builder.build();
    }

    /**
     * Update notification
     */
    private void updateNotification(String content, int progress) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, buildNotification(content, progress));
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    /**
     * Download listener interface
     */
    public interface DownloadListener {
        void onNovelDownloaded(Novel novel);
        void onChapterDownloaded(Chapter chapter);
        void onDownloadError(String id, String error);
    }
}
