package com.nav.navigator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.nav.navigator.data.AppRepository;
import com.nav.navigator.model.FuelRecord;
import com.nav.navigator.model.ParkingLocation;
import com.nav.navigator.model.RideRecord;
import com.nav.navigator.service.StreamingService;
import com.nav.navigator.wallpaper.WallpaperActivity;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private MapView mapView;
    private MapLibreMap currentMap;
    private LocationManager locationManager;
    private AppRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isRiding = false;
    private long rideStartTime = 0;
    private double rideDistanceKm = 0.0;
    private Location lastRideLocation = null;

    private boolean isSatelliteView = false;
    private LinearLayout destinationCard;
    private TextView tvDestinationInfo;
    private ListView suggestionListView;
    private ArrayAdapter<String> suggestionAdapter;
    private final List<Address> lastSearchedAddresses = new ArrayList<>();
    private final List<String> suggestionTexts = new ArrayList<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        repository = new AppRepository(this);

        // Root FrameLayout
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 1. MapView in background
        mapView = new MapView(this);
        mapView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        rootLayout.addView(mapView);

        // Top container for Search Bar & Suggestions dropdown
        LinearLayout topContainer = new LinearLayout(this);
        topContainer.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        topParams.gravity = Gravity.TOP;
        topParams.setMargins(32, 120, 32, 32);
        topContainer.setLayoutParams(topParams);

        // 2. Search Bar Overlay
        LinearLayout searchBarLayout = new LinearLayout(this);
        searchBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchBarLayout.setPadding(20, 20, 20, 20);
        searchBarLayout.setBackgroundColor(0xEE111111); // Dark AMOLED theme

        EditText searchInput = new EditText(this);
        searchInput.setHint("Search location (e.g. Kollam)...");
        searchInput.setHintTextColor(0xFF888888);
        searchInput.setTextColor(0xFFFFFFFF);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        searchBarLayout.addView(searchInput);

        Button searchButton = new Button(this);
        searchButton.setText("Search");
        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                if (currentMap != null) {
                    Toast.makeText(this, "Searching for " + query + "...", Toast.LENGTH_SHORT).show();
                    new Thread(() -> performSearch(query)).start();
                } else {
                    Toast.makeText(this, "Map is loading...", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
            }
        });
        searchBarLayout.addView(searchButton);
        topContainer.addView(searchBarLayout);

        // Suggestion List View (dropdown below search bar)
        suggestionListView = new ListView(this);
        suggestionListView.setBackgroundColor(0xEE222222);
        suggestionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestionTexts);
        suggestionListView.setAdapter(suggestionAdapter);
        suggestionListView.setVisibility(View.GONE);
        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < lastSearchedAddresses.size()) {
                Address selectedAddress = lastSearchedAddresses.get(position);
                selectDestination(selectedAddress);
                suggestionListView.setVisibility(View.GONE);
            }
        });
        topContainer.addView(suggestionListView);

        rootLayout.addView(topContainer);

        // Destination Info & Start Navigation Card (initially hidden)
        destinationCard = new LinearLayout(this);
        destinationCard.setOrientation(LinearLayout.VERTICAL);
        destinationCard.setPadding(24, 24, 24, 24);
        destinationCard.setBackgroundColor(0xEE111111);
        destinationCard.setVisibility(View.GONE);

        FrameLayout.LayoutParams destParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        destParams.gravity = Gravity.TOP;
        destParams.setMargins(32, 280, 32, 32);
        destinationCard.setLayoutParams(destParams);

        tvDestinationInfo = new TextView(this);
        tvDestinationInfo.setTextColor(0xFFFFFFFF);
        tvDestinationInfo.setTextSize(16f);
        destinationCard.addView(tvDestinationInfo);

        Button btnStartNav = new Button(this);
        btnStartNav.setText("Start Navigation");
        btnStartNav.setBackgroundColor(0xFF2E7D32); // Green
        btnStartNav.setTextColor(0xFFFFFFFF);
        btnStartNav.setOnClickListener(v -> {
            Toast.makeText(this, "Navigation Started! Follow route on map.", Toast.LENGTH_LONG).show();
            destinationCard.setVisibility(View.GONE);
        });
        destinationCard.addView(btnStartNav);

        rootLayout.addView(destinationCard);

        // 3. Bottom Control Panel (Connect, Ride, Style Toggle, Parking, Fuel, Wallpaper, Recenter)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setPadding(16, 16, 16, 16);
        bottomBar.setBackgroundColor(0xEE111111); // Dark AMOLED theme

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        bottomParams.gravity = Gravity.BOTTOM;
        bottomParams.setMargins(32, 32, 32, 32);
        bottomBar.setLayoutParams(bottomParams);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);

        Button btnConnect = new Button(this);
        btnConnect.setText("Connect Bike");
        btnConnect.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, StreamingService.class);
                intent.setAction("com.nav.navigator.ENABLE");
                startService(intent);
                Toast.makeText(this, "Connecting to TVS RTX 300...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        row1.addView(btnConnect);

        Button btnRide = new Button(this);
        btnRide.setText("Start Ride");
        btnRide.setOnClickListener(v -> {
            if (!isRiding) {
                isRiding = true;
                rideStartTime = System.currentTimeMillis();
                rideDistanceKm = 0.0;
                lastRideLocation = null;
                btnRide.setText("End Ride");
                Toast.makeText(this, "Ride recording started!", Toast.LENGTH_SHORT).show();
            } else {
                isRiding = false;
                long duration = System.currentTimeMillis() - rideStartTime;
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                RideRecord record = new RideRecord(
                        "ride_" + System.currentTimeMillis(),
                        rideStartTime,
                        System.currentTimeMillis(),
                        duration,
                        rideDistanceKm,
                        60.0,
                        30.0,
                        dateStr
                );
                repository.saveRide(record);
                btnRide.setText("Start Ride");
                Toast.makeText(this, "Ride saved! Dist: " + String.format(Locale.getDefault(), "%.2f km", rideDistanceKm), Toast.LENGTH_LONG).show();
            }
        });
        row1.addView(btnRide);

        Button btnStyle = new Button(this);
        btnStyle.setText("Map Style");
        btnStyle.setOnClickListener(v -> {
            isSatelliteView = !isSatelliteView;
            if (currentMap != null) {
                currentMap.setStyle("https://demotiles.maplibre.org/style.json", style -> Toast.makeText(this, isSatelliteView ? "Satellite / Custom Style" : "Standard Style", Toast.LENGTH_SHORT).show());
            }
        });
        row1.addView(btnStyle);

        bottomBar.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        Button btnRecenter = new Button(this);
        btnRecenter.setText("Recenter");
        btnRecenter.setOnClickListener(v -> recenterToCurrentLocation());
        row2.addView(btnRecenter);

        Button btnParking = new Button(this);
        btnParking.setText("Save Parking");
        btnParking.setOnClickListener(v -> {
            Location loc = getLastKnownLocation();
            if (loc != null) {
                ParkingLocation parking = new ParkingLocation(loc.getLatitude(), loc.getLongitude(), new Date().toString(), "Saved Parking Spot");
                repository.saveParkingLocation(parking);
                Toast.makeText(this, "Parking location saved!", Toast.LENGTH_SHORT).show();
                if (currentMap != null) {
                    currentMap.addMarker(new MarkerOptions()
                            .position(new LatLng(loc.getLatitude(), loc.getLongitude()))
                            .title("Parked Bike")
                            .snippet("Tap to view"));
                }
            } else {
                Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
            }
        });
        row2.addView(btnParking);

        Button btnWallpaper = new Button(this);
        btnWallpaper.setText("Wallpaper");
        btnWallpaper.setOnClickListener(v -> startActivity(new Intent(this, WallpaperActivity.class)));
        row2.addView(btnWallpaper);

        bottomBar.addView(row2);
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, this);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, this);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationManager == null) {
            return null;
        }
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return loc;
    }

    private void recenterToCurrentLocation() {
        Location loc = getLastKnownLocation();
        if (loc != null && currentMap != null) {
            LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            currentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0), 1500);
            Toast.makeText(this, "Centered to current location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Acquiring GPS position...", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch(String query) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(query, 5);
            lastSearchedAddresses.clear();
            suggestionTexts.clear();

            if (addresses != null && !addresses.isEmpty()) {
                Location currentLoc = getLastKnownLocation();
                for (Address address : addresses) {
                    lastSearchedAddresses.add(address);
                    String placeName = address.getAddressLine(0) != null ? address.getAddressLine(0) : query;
                    String displayText = placeName;

                    if (currentLoc != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), address.getLatitude(), address.getLongitude(), results);
                        double km = results[0] / 1000.0;
                        displayText += String.format(Locale.getDefault(), " (%.1f km away)", km);
                    }
                    suggestionTexts.add(displayText);
                }

                mainHandler.post(() -> {
                    suggestionAdapter.notifyDataSetChanged();
                    suggestionListView.setVisibility(View.VISIBLE);
                });
            } else {
                mainHandler.post(() -> {
                    suggestionListView.setVisibility(View.GONE);
                    Toast.makeText(this, "Location not found: " + query, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (IOException e) {
            mainHandler.post(() -> Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void selectDestination(Address address) {
        double lat = address.getLatitude();
        double lng = address.getLongitude();
        String placeName = address.getAddressLine(0) != null ? address.getAddressLine(0) : "Selected Location";

        if (currentMap != null) {
            currentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 14.0), 2000);
            currentMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .title(placeName)
                    .snippet("Destination"));

            String infoText = "Destination: " + placeName;
            Location currentLoc = getLastKnownLocation();
            if (currentLoc != null) {
                float[] results = new float[1];
                Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), lat, lng, results);
                double km = results[0] / 1000.0;
                infoText += String.format(Locale.getDefault(), "\nDistance: %.1f km from current GPS", km);
            }

            tvDestinationInfo.setText(infoText);
            destinationCard.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isRiding) {
            if (lastRideLocation != null) {
                double dist = lastRideLocation.distanceTo(location) / 1000.0; // km
                if (dist > 0.005) { // filter micro-jitters
                    rideDistanceKm += dist;
                }
            }
            lastRideLocation = location;
        }
    }

    @Override
    public void onMapReady(MapLibreMap mapLibreMap) {
        currentMap = mapLibreMap;
        mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json", style -> {
            // Map style loaded successfully
            recenterToCurrentLocation();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        mapView.onDestroy();
    }
}
