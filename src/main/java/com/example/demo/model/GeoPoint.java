package com.example.demo.model;

public class GeoPoint {
    private double lat;
    private double lon;
    private String source; // "GPS" ili "IP" ili "DEFAULT_DEV"

    public GeoPoint(double lat, double lon, String source) {
        this.lat = lat;
        this.lon = lon;
        this.source = source;
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getSource() { return source; }
}
