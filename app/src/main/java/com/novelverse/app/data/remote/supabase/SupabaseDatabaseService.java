package com.novelverse.app.data.remote.supabase;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Low-level Supabase PostgREST service. All methods are async (OkHttp enqueue). */
public class SupabaseDatabaseService {

    private static final String TAG = "SupabaseDB";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final SupabaseClient client;
    private final OkHttpClient http;
    private final Gson gson = new Gson();

    public SupabaseDatabaseService(SupabaseClient client) {
        this.client = client;
        this.http =
                new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();
    }

    // ── Callback ──────────────────────────────────────────────────────────

    public interface DatabaseCallback {
        void onSuccess(String result);

        void onError(String error);
    }

    // ── Base request builder ──────────────────────────────────────────────

    private Request.Builder base(String url, String token) {
        return new Request.Builder()
                .url(url)
                .header("apikey", client.getAnonKey())
                .header("Authorization", "Bearer " + (token != null ? token : client.getAnonKey()))
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation");
    }

    private void enqueue(Call call, DatabaseCallback cb) {
        call.enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call c, IOException e) {
                        Log.e(TAG, "Network failure", e);
                        cb.onError(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call c, Response r) throws IOException {
                        String body = r.body() != null ? r.body().string() : "";
                        if (r.isSuccessful()) {
                            cb.onSuccess(body);
                        } else {
                            Log.e(TAG, "HTTP " + r.code() + ": " + body);
                            cb.onError("HTTP " + r.code());
                        }
                    }
                });
    }

    // ── SELECT ────────────────────────────────────────────────────────────

    public void select(String table, String select, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table + "?select=" + urlEncode(select);
        enqueue(http.newCall(base(url, token).get().build()), cb);
    }

    public void selectWhere(
            String table,
            String select,
            String filter,
            String order,
            String token,
            DatabaseCallback cb) {
        StringBuilder url =
                new StringBuilder(client.getRestUrl())
                        .append("/")
                        .append(table)
                        .append("?select=")
                        .append(urlEncode(select));
        if (filter != null && !filter.isEmpty()) url.append("&").append(filter);
        if (order != null && !order.isEmpty()) url.append("&order=").append(order);
        enqueue(http.newCall(base(url.toString(), token).get().build()), cb);
    }

    public void selectById(String table, String id, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table + "?id=eq." + id + "&select=*";
        enqueue(
                http.newCall(
                        base(url, token)
                                .header("Accept", "application/vnd.pgrst.object+json")
                                .get()
                                .build()),
                cb);
    }

    public void queryTable(String table, String query, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table + "?" + query;
        enqueue(http.newCall(base(url, null).get().build()), cb);
    }

    // ── INSERT ────────────────────────────────────────────────────────────

    public void insert(String table, JsonObject data, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table;
        RequestBody rb = RequestBody.create(JSON, data.toString());
        enqueue(http.newCall(base(url, token).post(rb).build()), cb);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public void update(
            String table, String id, JsonObject data, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table + "?id=eq." + id;
        RequestBody rb = RequestBody.create(JSON, data.toString());
        enqueue(
                http.newCall(
                        base(url, token)
                                .header("Prefer", "return=representation")
                                .patch(rb)
                                .build()),
                cb);
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(String table, String id, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table + "?id=eq." + id;
        enqueue(http.newCall(base(url, token).delete().build()), cb);
    }

    /**
     * DELETE rows matching a PostgREST filter string. e.g. filter =
     * "user_id=eq.UUID&novel_id=eq.UUID"
     */
    public void deleteWhere(String table, String filter, String token, DatabaseCallback cb) {
        StringBuilder url = new StringBuilder(client.getRestUrl()).append("/").append(table);
        if (filter != null && !filter.isEmpty()) url.append("?").append(filter);
        enqueue(http.newCall(base(url.toString(), token).delete().build()), cb);
    }

    // ── UPSERT ───────────────────────────────────────────────────────────

    /** Simple upsert (no conflict column — Supabase uses PK by default). */
    public void upsert(String table, JsonObject data, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/" + table;
        RequestBody rb = RequestBody.create(JSON, data.toString());
        enqueue(
                http.newCall(
                        new Request.Builder()
                                .url(url)
                                .header("apikey", client.getAnonKey())
                                .header(
                                        "Authorization",
                                        "Bearer " + (token != null ? token : client.getAnonKey()))
                                .header("Content-Type", "application/json")
                                .header(
                                        "Prefer",
                                        "resolution=merge-duplicates,return=representation")
                                .post(rb)
                                .build()),
                cb);
    }

    /**
     * Upsert with explicit conflict columns (e.g. "user_id,novel_id"). PostgREST uses on_conflict
     * query param to specify which columns to check for duplication → INSERT or UPDATE accordingly.
     */
    public void upsert(
            String table, JsonObject data, String onConflict, String token, DatabaseCallback cb) {
        StringBuilder url = new StringBuilder(client.getRestUrl()).append("/").append(table);
        if (onConflict != null && !onConflict.isEmpty()) {
            url.append("?on_conflict=").append(onConflict);
        }
        RequestBody rb = RequestBody.create(JSON, data.toString());
        enqueue(
                http.newCall(
                        new Request.Builder()
                                .url(url.toString())
                                .header("apikey", client.getAnonKey())
                                .header(
                                        "Authorization",
                                        "Bearer " + (token != null ? token : client.getAnonKey()))
                                .header("Content-Type", "application/json")
                                .header(
                                        "Prefer",
                                        "resolution=merge-duplicates,return=representation")
                                .post(rb)
                                .build()),
                cb);
    }

    // ── Upload image to Storage ───────────────────────────────────────────

    public void uploadFile(
            String bucket,
            String path,
            byte[] data,
            String contentType,
            String token,
            DatabaseCallback cb) {
        String url = client.getStorageUrl() + "/object/" + bucket + "/" + path;
        MediaType mt =
                MediaType.get(contentType != null ? contentType : "application/octet-stream");
        RequestBody rb = RequestBody.create(mt, data);
        enqueue(
                http.newCall(
                        new Request.Builder()
                                .url(url)
                                .header("apikey", client.getAnonKey())
                                .header(
                                        "Authorization",
                                        "Bearer " + (token != null ? token : client.getAnonKey()))
                                .header("x-upsert", "true")
                                .post(rb)
                                .build()),
                cb);
    }

    // ── RPC ───────────────────────────────────────────────────────────────

    public void callRpc(String funcName, JsonObject params, String token, DatabaseCallback cb) {
        String url = client.getRestUrl() + "/rpc/" + funcName;
        RequestBody rb = RequestBody.create(JSON, params != null ? params.toString() : "{}");
        enqueue(
                http.newCall(
                        new Request.Builder()
                                .url(url)
                                .header("apikey", client.getAnonKey())
                                .header(
                                        "Authorization",
                                        "Bearer " + (token != null ? token : client.getAnonKey()))
                                .header("Content-Type", "application/json")
                                .post(rb)
                                .build()),
                cb);
    }

    // ── Public storage URL ────────────────────────────────────────────────

    /**
     * Returns the public URL for a file in a public Supabase Storage bucket. Pattern:
     * {supabaseUrl}/storage/v1/object/public/{bucket}/{path} Used by NovelRepository.uploadCover()
     * and UserRepository avatar upload.
     */
    public String getPublicUrl(String bucket, String path) {
        return client.getStorageUrl() + "/object/public/" + bucket + "/" + path;
    }


    // ── Flexible SELECT with custom select columns ─────────────────────────

    /** Build a PostgREST URL with select and optional filter/order strings. */
    public String buildSelectUrl(String table, String select, String filter, String order) {
        StringBuilder url = new StringBuilder(client.getRestUrl())
                .append("/").append(table)
                .append("?select=").append(urlEncode(select));
        if (filter != null && !filter.isEmpty()) url.append("&").append(filter);
        if (order  != null && !order.isEmpty())  url.append("&order=").append(order);
        return url.toString();
    }

    /** GET request to a fully-formed URL (used when caller builds the URL itself). */
    public void rawSelect(String url, String token, DatabaseCallback cb) {
        enqueue(http.newCall(base(url, token).get().build()), cb);
    }

    /** POST to /rpc/<functionName> — used for Postgres functions / view increments. */
    public void rpcPost(String functionName, JsonObject params, String token, DatabaseCallback cb) {
        String url = client.getRestUrl().replace("/rest/v1", "/rest/v1/rpc") + "/" + functionName;
        RequestBody body = RequestBody.create(params != null ? gson.toJson(params) : "{}", JSON);
        enqueue(http.newCall(base(url, token).post(body).build()), cb);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
