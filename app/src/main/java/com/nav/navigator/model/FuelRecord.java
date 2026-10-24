package com.nav.navigator.model;

import java.io.Serializable;

public class FuelRecord implements Serializable {
    private String date;
    private double odometerKm;
    private double liters;
    private double costTotal;

    public FuelRecord(String date, double odometerKm, double liters, double costTotal) {
        this.date = date;
        this.odometerKm = odometerKm;
        this.liters = liters;
        this.costTotal = costTotal;
    }

    public String getDate() { return date; }
    public double getOdometerKm() { return odometerKm; }
    public double getLiters() { return liters; }
    public double getCostTotal() { return costTotal; }
}
