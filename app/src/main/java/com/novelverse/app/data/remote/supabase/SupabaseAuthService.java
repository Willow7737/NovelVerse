package com.novelverse.app.data.remote.supabase;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.novelverse.app.domain.models.AuthResult;
import com.novelverse.app.domain.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for Supabase authentication operations
 */
public class SupabaseAuthService {

    private static final String TAG = "SupabaseAuthService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final SupabaseClient client;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public SupabaseAuthService(SupabaseClient client, OkHttpClient httpClient) {
        this.client = client;
        this.httpClient = httpClient;
        this.gson = new Gson();
    }

    /**
     * Sign up with email and password
     */
    public void signUp(String email, String password, String username, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        json.add("data", data);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/signup")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Sign up failed", e);
                callback.onResult(AuthResult.error(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAuthResponse(response, callback);
            }
        });
    }

    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/token?grant_type=password")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Sign in failed", e);
                callback.onResult(AuthResult.error(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAuthResponse(response, callback);
            }
        });
    }

    /**
     * Sign in with a Google ID token.
     *
     * FIX: The old signInWithOAuth() hit Supabase's browser-redirect endpoint
     * (/auth/v1/authorize) which is designed for web flows and cannot return a
     * session token directly to a native Android app.
     *
     * This method uses the correct endpoint for native apps:
     *   POST /auth/v1/token?grant_type=id_token
     *   body: { "provider": "google", "id_token": "<token from GoogleSignInAccount>" }
     *
     * Supabase validates the token with Google, then returns a full session
     * (access_token, refresh_token, user) just like a password sign-in.
     *
     * Requirements on the Supabase dashboard:
     *   Authentication → Providers → Google → enable "Google" and paste your
     *   Web Client ID + Web Client Secret from Google Cloud Console.
     */
    public void signInWithGoogleToken(String idToken, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("provider", "google");
        json.addProperty("id_token", idToken);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/token?grant_type=id_token")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Google token sign-in failed", e);
                callback.onResult(AuthResult.error(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAuthResponse(response, callback);
            }
        });
    }

    /**
     * Sign out
     */
    public void signOut(String accessToken, SimpleCallback callback) {
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/logout")
            .header("apikey", client.getAnonKey())
            .header("Authorization", "Bearer " + accessToken)
            .post(RequestBody.create(JSON, ""))
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Sign out failed", e);
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    /**
     * Refresh access token
     */
    public void refreshToken(String refreshToken, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("refresh_token", refreshToken);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/token?grant_type=refresh_token")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Token refresh failed", e);
                callback.onResult(AuthResult.error(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAuthResponse(response, callback);
            }
        });
    }

    /**
     * Send password reset email
     */
    public void resetPassword(String email, SimpleCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/recover")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Password reset failed", e);
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    /**
     * Resend verification email
     */
    public void resendVerification(String email, SimpleCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("type", "signup");

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/resend")
            .header("apikey", client.getAnonKey())
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Resend verification failed", e);
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    /**
     * Update user password
     */
    public void updatePassword(String accessToken, String newPassword, SimpleCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("password", newPassword);

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/user")
            .header("apikey", client.getAnonKey())
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .put(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Update password failed", e);
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    /**
     * Get current user
     */
    public void getCurrentUser(String accessToken, UserCallback callback) {
        Request request = new Request.Builder()
            .url(client.getAuthUrl() + "/user")
            .header("apikey", client.getAnonKey())
            .header("Authorization", "Bearer " + accessToken)
            .get()
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Get user failed", e);
                callback.onResult(null, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        User user = parseUserFromJson(json);
                        callback.onResult(user, null);
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse user failed", e);
                        callback.onResult(null, e.getMessage());
                    }
                } else {
                    callback.onResult(null, "Failed to get user: " + response.code());
                }
            }
        });
    }

    /**
     * Handle authentication response.
     *
     * Supabase returns three distinct shapes:
     *  1. Full session  — { access_token, refresh_token, expires_in, user: {...} }
     *  2. Email pending — { user: {...}, session: null }  (email confirmation required)
     *  3. Error         — { error, error_description/message }
     */
    private void handleAuthResponse(Response response, AuthCallback callback) {
        if (response.isSuccessful() && response.body() != null) {
            try {
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                if (json.has("access_token")) {
                    JSONObject userJson = json.has("user")
                            ? json.getJSONObject("user")
                            : json;
                    User user = parseUserFromJson(userJson);
                    String accessToken = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", "");
                    long expiresIn = json.optLong("expires_in", 3600);
                    callback.onResult(AuthResult.success(user, accessToken, refreshToken, expiresIn));

                } else if (json.has("user")) {
                    callback.onResult(AuthResult.error(
                            "Account created! Please check your email to verify your address, then log in."));

                } else {
                    String msg = json.optString("error_description",
                                 json.optString("message",
                                 json.optString("error", "Authentication failed. Please try again.")));
                    callback.onResult(AuthResult.error(msg));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Parse auth response failed", e);
                callback.onResult(AuthResult.error("Failed to parse server response."));
            } catch (IOException e) {
                Log.e(TAG, "Read response failed", e);
                callback.onResult(AuthResult.error("Network error. Please try again."));
            }
        } else {
            try {
                String errorBody = response.body() != null ? response.body().string() : "";
                if (!errorBody.isEmpty()) {
                    JSONObject errorJson = new JSONObject(errorBody);
                    String msg = errorJson.optString("error_description",
                                 errorJson.optString("message",
                                 errorJson.optString("error", "Authentication failed: " + response.code())));
                    callback.onResult(AuthResult.error(msg));
                } else {
                    callback.onResult(AuthResult.error("Authentication failed: " + response.code()));
                }
            } catch (Exception e) {
                callback.onResult(AuthResult.error("Authentication failed: " + response.code()));
            }
        }
    }

    /**
     * Parse user from JSON
     */
    private User parseUserFromJson(JSONObject json) throws JSONException {
        User user = new User();
        user.setId(json.optString("id", UUID.randomUUID().toString()));
        user.setEmail(json.optString("email", ""));

        JSONObject userMetadata = json.optJSONObject("user_metadata");
        if (userMetadata != null) {
            user.setUsername(userMetadata.optString("username", ""));
            user.setDisplayName(userMetadata.optString("display_name", ""));
            user.setAvatarUrl(userMetadata.optString("avatar_url", ""));
            user.setRole(userMetadata.optString("role", "reader"));
        }

        user.setEmailVerified(json.optBoolean("email_confirmed_at", false));

        String createdAt = json.optString("created_at", null);
        if (createdAt != null) {
            user.setCreatedAt(new Date());
        }

        return user;
    }

    /**
     * Create a guest user
     */
    public User createGuestUser() {
        User guest = new User();
        guest.setId("guest_" + UUID.randomUUID().toString());
        guest.setUsername("guest");
        guest.setDisplayName("Guest");
        guest.setRole("guest");
        guest.setEmail("");
        return guest;
    }

    // Callback interfaces
    public interface AuthCallback {
        void onResult(AuthResult result);
    }

    public interface SimpleCallback {
        void onResult(boolean success, String error);
    }

    public interface UserCallback {
        void onResult(User user, String error);
    }
}
