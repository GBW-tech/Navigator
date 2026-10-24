package com.nav.navigator.model;

import java.io.Serializable;

public class ParkingLocation implements Serializable {
    private double latitude;
    private double longitude;
    private String timestamp;
    private String address;

    public ParkingLocation(double latitude, double longitude, String timestamp, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.address = address;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimestamp() { return timestamp; }
    public String getAddress() { return address; }
}
