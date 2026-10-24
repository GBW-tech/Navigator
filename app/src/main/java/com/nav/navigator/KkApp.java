package com.nav.navigator;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import com.nav.navigator.core.Protocol;
import org.maplibre.android.MapLibre;

public class KkApp extends Application {
    private static final String TAG = "KkApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            MapLibre.getInstance(this);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize MapLibre", t);
        }
        try {
            Protocol.init(this);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize Protocol", t);
        }
        try {
            SharedPreferences prefs = getSharedPreferences("rides", 0);
            boolean ridesEnabled = prefs.getBoolean("enabled", true);
            Log.i(TAG, "Rides enabled: " + ridesEnabled);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize rides preference", t);
        }
    }
}
