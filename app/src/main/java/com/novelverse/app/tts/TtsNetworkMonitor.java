package com.novelverse.app.tts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight network monitor used by the TTS system to decide whether
 * to route synthesis requests to Oracle Cloud (online) or fall back to
 * Android's built-in TTS (offline).
 *
 * Only used for connectivity state — no data is sent here.
 * When connectivity changes, registered listeners are notified on the main thread.
 */
public class TtsNetworkMonitor {

    public interface Listener {
        void onNetworkChanged(boolean isOnline);
    }

    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isOnline = false;

    public TtsNetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager)
            context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        isOnline = checkCurrentConnectivity();
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Returns true if the device currently has an active internet connection */
    public boolean isOnline() {
        return isOnline;
    }

    /** Returns true if the current connection appears to be metered (mobile data) */
    public boolean isMetered() {
        Network net = connectivityManager.getActiveNetwork();
        if (net == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
        if (caps == null) return false;
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }

    /**
     * Returns a rough quality estimate of the current connection.
     *  "none"   → no connectivity
     *  "poor"   → cellular 2G/3G
     *  "good"   → cellular 4G/5G or WiFi
     *  "great"  → WiFi or Ethernet with high bandwidth
     */
    public String getConnectionQuality() {
        if (!isOnline) return "none";
        Network net = connectivityManager.getActiveNetwork();
        if (net == null) return "none";
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
        if (caps == null) return "none";

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            int bw = caps.getLinkDownstreamBandwidthKbps();
            return bw > 5_000 ? "great" : "good";
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            int bw = caps.getLinkDownstreamBandwidthKbps();
            return bw > 1_000 ? "good" : "poor";
        }
        return "good";
    }

    // ── Listener management ───────────────────────────────────────────────

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /** Call from Service.onCreate() to start watching for connectivity changes */
    public void start() {
        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                updateState(true);
            }

            @Override
            public void onLost(Network network) {
                // Recheck — another network might still be available
                updateState(checkCurrentConnectivity());
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                boolean online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                              && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                updateState(online);
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    /** Call from Service.onDestroy() */
    public void stop() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
        listeners.clear();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private boolean checkCurrentConnectivity() {
        Network net = connectivityManager.getActiveNetwork();
        if (net == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
        return caps != null
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void updateState(boolean online) {
        if (online == isOnline) return; // no change
        isOnline = online;
        mainHandler.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onNetworkChanged(online);
            }
        });
    }
}
