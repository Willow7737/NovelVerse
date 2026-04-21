package com.novelverse.app.data.remote.supabase;

import android.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Service for Supabase storage operations
 */
public class SupabaseStorageService {

    private static final String TAG = "SupabaseStorageService";

    private final SupabaseClient client;
    private final OkHttpClient httpClient;

    public SupabaseStorageService(SupabaseClient client) {
        this.client = client;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Upload a file to storage
     */
    public void uploadFile(String bucket, String path, byte[] data, String contentType, String authToken, UploadCallback callback) {
        String url = client.getStorageUrl() + "/object/" + bucket + "/" + path;

        RequestBody body = RequestBody.create(MediaType.parse(contentType), data);
        Request request = new Request.Builder()
            .url(url)
            .header("apikey", client.getAnonKey())
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Upload failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    String publicUrl = getPublicUrl(bucket, path);
                    callback.onSuccess(publicUrl);
                } else {
                    callback.onError("Upload failed: " + response.code());
                }
            }
        });
    }

    /**
     * Get public URL for a file
     */
    public String getPublicUrl(String bucket, String path) {
        return client.getSupabaseUrl() + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    /**
     * Delete a file from storage
     */
    public void deleteFile(String bucket, String path, String authToken, DeleteCallback callback) {
        String url = client.getStorageUrl() + "/object/" + bucket + "/" + path;

        Request request = new Request.Builder()
            .url(url)
            .header("apikey", client.getAnonKey())
            .header("Authorization", "Bearer " + authToken)
            .delete()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Delete failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Delete failed: " + response.code());
                }
            }
        });
    }

    // Callback interfaces
    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
