package com.example.demo.util;

public final class GeohashUtil {
    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    private GeohashUtil() {}

    public static String encode(double lat, double lon, int precision) {
        lat = clamp(lat, -90, 90);
        lon = wrapLon(lon);

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        boolean even = true;
        int bit = 0, ch = 0;
        StringBuilder geohash = new StringBuilder();

        while (geohash.length() < precision) {
            double mid;
            if (even) {
                mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (lon >= mid) { ch |= (1 << (4 - bit)); lonRange[0] = mid; }
                else { lonRange[1] = mid; }
            } else {
                mid = (latRange[0] + latRange[1]) / 2.0;
                if (lat >= mid) { ch |= (1 << (4 - bit)); latRange[0] = mid; }
                else { latRange[1] = mid; }
            }

            even = !even;
            if (bit < 4) bit++;
            else {
                geohash.append(BASE32.charAt(ch));
                bit = 0; ch = 0;
            }
        }
        return geohash.toString();
    }

    public static BBox decodeBBox(String geohash) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        boolean even = true;

        for (int i = 0; i < geohash.length(); i++) {
            int cd = BASE32.indexOf(geohash.charAt(i));
            for (int mask = 16; mask != 0; mask >>= 1) {
                if (even) refine(lonRange, (cd & mask) != 0);
                else refine(latRange, (cd & mask) != 0);
                even = !even;
            }
        }
        return new BBox(latRange[0], latRange[1], lonRange[0], lonRange[1]);
    }

    private static void refine(double[] range, boolean upper) {
        double mid = (range[0] + range[1]) / 2.0;
        if (upper) range[0] = mid; else range[1] = mid;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double wrapLon(double lon) {
        while (lon < -180) lon += 360;
        while (lon > 180) lon -= 360;
        return lon;
    }

    public static int precisionForRadiusKm(double radiusKm) {
        if (radiusKm <= 1) return 7;      // ~0.6 km
        if (radiusKm <= 5) return 6;      // ~2.4 km
        if (radiusKm <= 10) return 5;     // ~9.4 km
        if (radiusKm <= 20) return 4;     // ~39 km
        if (radiusKm <= 50) return 4;     // ~39 km
        return 3;                          // ~156 km
    }

    public record BBox(double minLat, double maxLat, double minLon, double maxLon) {}
}
