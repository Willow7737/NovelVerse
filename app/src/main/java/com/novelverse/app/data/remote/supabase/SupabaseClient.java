package com.novelverse.app.data.remote.supabase;

/**
 * Supabase client configuration
 */
public class SupabaseClient {

    private final String supabaseUrl;
    private final String anonKey;

    public SupabaseClient(String supabaseUrl, String anonKey) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getAnonKey() {
        return anonKey;
    }

    public String getRestUrl() {
        return supabaseUrl + "/rest/v1";
    }

    public String getAuthUrl() {
        return supabaseUrl + "/auth/v1";
    }

    public String getStorageUrl() {
        return supabaseUrl + "/storage/v1";
    }

    public String getRealtimeUrl() {
        return supabaseUrl.replace("https://", "wss://") + "/realtime/v1";
    }
}
