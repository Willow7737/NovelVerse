package com.novelverse.app.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.novelverse.app.data.local.dao.NovelDao;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Worker for cleaning up old cached data
 */
@HiltWorker
public class CleanupWorker extends Worker {

    private static final String TAG = "CleanupWorker";
    private static final long CACHE_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    private final NovelDao novelDao;

    @AssistedInject
    public CleanupWorker(
            @Assisted Context context,
            @Assisted WorkerParameters params,
            NovelDao novelDao) {
        super(context, params);
        this.novelDao = novelDao;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting cleanup work...");
        
        try {
            // Delete old cache
            long cutoffTime = System.currentTimeMillis() - CACHE_EXPIRY_MS;
            novelDao.deleteOldCache(cutoffTime);
            
            Log.d(TAG, "Cleanup work completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Cleanup work failed", e);
            return Result.failure();
        }
    }
}
