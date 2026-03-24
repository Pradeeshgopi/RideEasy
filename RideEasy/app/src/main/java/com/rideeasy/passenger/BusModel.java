package com.rideeasy.passenger;

/** Holds live bus data received from backend or Firebase. */
public class BusModel {
    public String busNumber;
    public String conductorId;
    public String numberPlate;
    public String route;
    public int    totalPassengers;
    public int    totalSeats;
    public int    occupancyPercent;
    public String crowdStatus;
    public double latitude;
    public double longitude;
    public double speed;

    public BusModel() {}

    public BusModel(String busNumber, String conductorId, String numberPlate,
                    String route, int totalPassengers) {
        this.busNumber       = busNumber;
        this.conductorId     = conductorId;
        this.numberPlate     = numberPlate;
        this.route           = route;
        this.totalPassengers = totalPassengers;
        this.totalSeats      = AppConfig.TOTAL_SEATS;
        int pct              = Math.min(100, (int)((totalPassengers / (float) totalSeats) * 100));
        this.occupancyPercent = pct;
        if      (pct < AppConfig.CROWD_FREE_THRESHOLD)      crowdStatus = "FREE";
        else if (pct < AppConfig.CROWD_CROWDED_THRESHOLD)   crowdStatus = "MODERATE";
        else                                                  crowdStatus = "CROWDED";
    }
}
