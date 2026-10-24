package com.nav.navigator.model;

import java.io.Serializable;

public class RideRecord implements Serializable {
    private String id;
    private long startTime;
    private long endTime;
    private long durationMillis;
    private double distanceKm;
    private double maxSpeedKmh;
    private double avgSpeedKmh;
    private String dateStr;

    public RideRecord(String id, long startTime, long endTime, long durationMillis, double distanceKm, double maxSpeedKmh, double avgSpeedKmh, String dateStr) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = durationMillis;
        this.distanceKm = distanceKm;
        this.maxSpeedKmh = maxSpeedKmh;
        this.avgSpeedKmh = avgSpeedKmh;
        this.dateStr = dateStr;
    }

    public String getId() { return id; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDurationMillis() { return durationMillis; }
    public double getDistanceKm() { return distanceKm; }
    public double getMaxSpeedKmh() { return maxSpeedKmh; }
    public double getAvgSpeedKmh() { return avgSpeedKmh; }
    public String getDateStr() { return dateStr; }
}
