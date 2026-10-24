package com.nav.navigator.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nav.navigator.model.FuelRecord;
import com.nav.navigator.model.ParkingLocation;
import com.nav.navigator.model.RideRecord;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppRepository {
    private static final String PREF_NAME = "navigator_prefs";
    private static final String KEY_RIDES = "rides_list";
    private static final String KEY_FUEL = "fuel_list";
    private static final String KEY_PARKING = "parking_location";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public AppRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<RideRecord> getRides() {
        String json = prefs.getString(KEY_RIDES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<RideRecord>>(){}.getType();
        List<RideRecord> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void saveRide(RideRecord ride) {
        List<RideRecord> rides = getRides();
        rides.add(0, ride);
        prefs.edit().putString(KEY_RIDES, gson.toJson(rides)).apply();
    }

    public List<FuelRecord> getFuelRecords() {
        String json = prefs.getString(KEY_FUEL, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<FuelRecord>>(){}.getType();
        List<FuelRecord> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void saveFuelRecord(FuelRecord record) {
        List<FuelRecord> records = getFuelRecords();
        records.add(0, record);
        prefs.edit().putString(KEY_FUEL, gson.toJson(records)).apply();
    }

    public ParkingLocation getParkingLocation() {
        String json = prefs.getString(KEY_PARKING, null);
        if (json == null) return null;
        return gson.fromJson(json, ParkingLocation.class);
    }

    public void saveParkingLocation(ParkingLocation parking) {
        prefs.edit().putString(KEY_PARKING, gson.toJson(parking)).apply();
    }

    public void clearParkingLocation() {
        prefs.edit().remove(KEY_PARKING).apply();
    }
}
