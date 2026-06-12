package com.novelverse.app.data.remote.supabase;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.novelverse.app.domain.models.AuthResult;
import com.novelverse.app.domain.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
 * Supabase authentication service.
 *
 * Provider support:
 *  - Email/password — standard sign-up / sign-in.
 *  - Google (native) — id_token grant with nonce validation.
 *  - Facebook / Apple / others — PKCE authorization-code exchange.
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

    // ── PKCE helpers ──────────────────────────────────────────────────────────

    /** Generates a cryptographically secure PKCE code verifier (RFC 7636). */
    public static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    /** Derives the PKCE code challenge from a verifier using S256. */
    public static String deriveCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes("US-ASCII"));
            return Base64.encodeToString(hash,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("PKCE derivation failed", e);
        }
    }

    // ── Nonce helpers (Google id_token) ───────────────────────────────────────

    /**
     * Generates a 32-byte cryptographically secure raw nonce (hex string).
     * Pass rawNonce to Supabase; pass sha256Hex(rawNonce) to Google requestNonce().
     */
    public static String generateRawNonce() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** SHA-256 hash of input, returned as a hex string. */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    public void signUp(String email, String password, String username, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        json.add("data", data);
        doPost(client.getAuthUrl() + "/signup", json, null, callback);
    }

    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        doPost(client.getAuthUrl() + "/token?grant_type=password", json, null, callback);
    }

    // ── Google native id_token flow ───────────────────────────────────────────

    /**
     * Exchanges a Google ID token for a Supabase session.
     *
     * IMPORTANT — nonce is required (Supabase validates it by default):
     *  1. generateRawNonce()  → store as rawNonce
     *  2. sha256Hex(rawNonce) → pass to GoogleSignInOptions.requestNonce()
     *  3. After Google returns, call this with idToken + rawNonce
     *
     * Endpoint: POST /auth/v1/token?grant_type=id_token
     */
    public void signInWithGoogleIdToken(String idToken, String rawNonce, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("provider", "google");
        json.addProperty("id_token", idToken);
        if (rawNonce != null && !rawNonce.isEmpty()) {
            json.addProperty("nonce", rawNonce);
        }
        doPost(client.getAuthUrl() + "/token?grant_type=id_token", json, null, callback);
    }

    // ── PKCE OAuth Code Exchange (Facebook, Apple, etc.) ─────────────────────

    /**
     * Exchanges a Supabase OAuth authorization code for a session.
     * Call after receiving code from the deep-link callback.
     *
     * Endpoint: POST /auth/v1/token?grant_type=pkce
     * Body: { "auth_code": "...", "code_verifier": "..." }
     */
    public void exchangeOAuthCode(String authCode, String codeVerifier, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("auth_code", authCode);
        json.addProperty("code_verifier", codeVerifier);
        doPost(client.getAuthUrl() + "/token?grant_type=pkce", json, null, callback);
    }

    /**
     * Builds the Supabase OAuth authorization URL for browser-based PKCE flow.
     * Open this in a CustomTabsIntent.
     *
     * @param provider      "facebook", "apple", "github", "discord", etc.
     * @param codeChallenge result of deriveCodeChallenge(codeVerifier)
     * @param redirectTo    deep-link URI: "novelverse://auth/callback"
     */
    public String buildOAuthUrl(String provider, String codeChallenge, String redirectTo) {
        return client.getAuthUrl()
                + "/authorize?provider=" + provider
                + "&redirect_to=" + android.net.Uri.encode(redirectTo)
                + "&flow_type=pkce"
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";
    }

    // ── Session management ────────────────────────────────────────────────────

    public void signOut(String accessToken, SimpleCallback callback) {
        Request request = new Request.Builder()
                .url(client.getAuthUrl() + "/logout")
                .header("apikey", client.getAnonKey())
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(JSON, ""))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(false, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    public void refreshToken(String refreshToken, AuthCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("refresh_token", refreshToken);
        doPost(client.getAuthUrl() + "/token?grant_type=refresh_token", json, null, callback);
    }

    // ── Password management ───────────────────────────────────────────────────

    public void resetPassword(String email, String redirectTo, SimpleCallback callback) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);

        String url = client.getAuthUrl() + "/recover";
        if (redirectTo != null && !redirectTo.isEmpty()) {
            url += "?redirect_to=" + android.net.Uri.encode(redirectTo);
        }

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", client.getAnonKey())
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(false, "Network error. Please check your connection.");
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onResult(true, null);
                } else {
                    try {
                        String body2 = response.body() != null ? response.body().string() : "";
                        JSONObject errorJson = new JSONObject(body2);
                        String code = errorJson.optString("error_code", "");
                        String msg  = resolveMessage(errorJson, null);
                        callback.onResult(false, mapErrorCodeToMessage(code, msg, response.code()));
                    } catch (Exception ex) {
                        callback.onResult(false, genericErrorForCode(response.code()));
                    }
                }
            }
        });
    }

    public void updatePasswordWithToken(String accessToken, String newPassword,
                                        SimpleCallback callback) {
        updatePassword(accessToken, newPassword, callback);
    }

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
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(false, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

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
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(false, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                callback.onResult(response.isSuccessful(), null);
            }
        });
    }

    public void getCurrentUser(String accessToken, UserCallback callback) {
        Request request = new Request.Builder()
                .url(client.getAuthUrl() + "/user")
                .header("apikey", client.getAnonKey())
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(null, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        callback.onResult(parseUserFromJson(json), null);
                    } catch (JSONException e) {
                        callback.onResult(null, "Failed to parse user data.");
                    }
                } else {
                    callback.onResult(null, "Failed to get user: " + response.code());
                }
            }
        });
    }

    public User createGuestUser() {
        User guest = new User();
        guest.setId("guest_" + UUID.randomUUID().toString());
        guest.setUsername("guest");
        guest.setDisplayName("Guest");
        guest.setRole("guest");
        guest.setEmail("");
        return guest;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void doPost(String url, JsonObject json, String accessToken, AuthCallback callback) {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("apikey", client.getAnonKey())
                .header("Content-Type", "application/json")
                .post(body);
        if (accessToken != null && !accessToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        httpClient.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Auth request failed: " + url, e);
                callback.onResult(AuthResult.error("Network error. Please check your connection."));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                handleAuthResponse(response, callback);
            }
        });
    }

    private void handleAuthResponse(Response response, AuthCallback callback) {
        if (response.isSuccessful() && response.body() != null) {
            try {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);

                if (json.has("access_token")) {
                    JSONObject userJson = json.has("user") ? json.getJSONObject("user") : json;
                    User user           = parseUserFromJson(userJson);
                    String accessToken  = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", "");
                    long expiresIn      = json.optLong("expires_in", 3600);
                    callback.onResult(AuthResult.success(user, accessToken, refreshToken, expiresIn));

                } else if (json.has("user")) {
                    User user = parseUserFromJson(json.getJSONObject("user"));
                    callback.onResult(AuthResult.pendingVerification(user));

                } else {
                    String msg = resolveMessage(json, "Authentication failed. Please try again.");
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
                    String errorCode = errorJson.optString("error_code", "");
                    String rawMsg    = resolveMessage(errorJson, null);
                    callback.onResult(AuthResult.error(
                            mapErrorCodeToMessage(errorCode, rawMsg, response.code())));
                } else {
                    callback.onResult(AuthResult.error(genericErrorForCode(response.code())));
                }
            } catch (Exception e) {
                callback.onResult(AuthResult.error(genericErrorForCode(response.code())));
            }
        }
    }

    private String resolveMessage(JSONObject json, String fallback) {
        String msg;
        msg = json.optString("msg", null);        if (msg != null && !msg.isEmpty()) return msg;
        msg = json.optString("error_description", null); if (msg != null && !msg.isEmpty()) return msg;
        msg = json.optString("message", null);    if (msg != null && !msg.isEmpty()) return msg;
        msg = json.optString("error", null);      if (msg != null && !msg.isEmpty()) return msg;
        return fallback;
    }

    private String mapErrorCodeToMessage(String code, String raw, int http) {
        if (code == null) code = "";
        switch (code) {
            case "over_email_send_rate_limit":
                if (raw != null && raw.contains("after "))
                    return "Too many attempts. " +
                            raw.substring(raw.indexOf("after ") - 1).trim() + " before trying again.";
                return "Too many attempts. Please wait before requesting another email.";
            case "over_request_rate_limit":
                return "You're moving too fast. Please wait a moment.";
            case "user_already_exists": case "email_exists":
                return "An account with this email already exists. Try signing in instead.";
            case "invalid_credentials":
                return "Incorrect email or password. Please check and try again.";
            case "email_not_confirmed":
                return "Please verify your email first. Check your inbox for the confirmation link.";
            case "weak_password":
                return "Your password is too weak. Use at least 8 characters with a mix of letters and numbers.";
            case "signup_disabled":
                return "New sign-ups are temporarily disabled. Please try again later.";
            case "provider_disabled":
                return "This sign-in method is currently unavailable. Please try another option.";
            case "bad_jwt": case "no_authorization":
                return "Your session has expired. Please sign in again.";
            default:
                if (raw != null && !raw.isEmpty()) return raw;
                return genericErrorForCode(http);
        }
    }

    private String genericErrorForCode(int http) {
        switch (http) {
            case 400: return "Invalid request. Please check your details.";
            case 401: return "Authentication failed. Please check your credentials.";
            case 403: return "Access denied. Please sign in again.";
            case 422: return "Invalid details provided. Please review and try again.";
            case 429: return "Too many attempts. Please wait before trying again.";
            case 500: case 502: case 503:
                return "Server trouble right now. Please try again in a moment.";
            default:  return "Something went wrong. Please try again.";
        }
    }

    private User parseUserFromJson(JSONObject json) throws JSONException {
        User user = new User();
        user.setId(json.optString("id", UUID.randomUUID().toString()));
        user.setEmail(json.optString("email", ""));

        JSONObject meta = json.optJSONObject("user_metadata");
        if (meta != null) {
            user.setUsername(meta.optString("username", ""));
            user.setDisplayName(meta.optString("display_name",
                    meta.optString("full_name", "")));
            user.setAvatarUrl(meta.optString("avatar_url", ""));
            user.setRole(meta.optString("role", "reader"));
        }

        user.setEmailVerified(json.has("email_confirmed_at")
                && !json.isNull("email_confirmed_at")
                && !json.optString("email_confirmed_at", "").isEmpty());

        if (json.has("created_at")) user.setCreatedAt(new Date());
        return user;
    }

    // ── Callback interfaces ───────────────────────────────────────────────────

    public interface AuthCallback { void onResult(AuthResult result); }
    public interface SimpleCallback { void onResult(boolean success, String error); }
    public interface UserCallback { void onResult(User user, String error); }
}
