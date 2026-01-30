package com.example.demo.model;


/**
 * Enumeracija za kvalitet videa
 * Koristi se za heurističko određivanje trajanja videa na osnovu bitrate-a
 */
public enum VideoQuality {
    LOW("Low", 0.6),      // 0.6 MB/s
    MEDIUM("Medium", 1.2), // 1.2 MB/s
    HIGH("High", 2.0);     // 2.0 MB/s

    private final String displayName;
    private final double megabytesPerSecond;
    private final double bitrateMBps;

    VideoQuality(String displayName, double megabytesPerSecond) {
        this.displayName = displayName;
        this.megabytesPerSecond = megabytesPerSecond;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMegabytesPerSecond() {
        return megabytesPerSecond;
    }

    /**
     * Proceni trajanje videa na osnovu veličine fajla i kvaliteta
     * formula: duration_seconds = file_size_MB / megabytes_per_second
     */
    public long estimateDurationSeconds(long fileSizeBytes) {
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        return Math.round(fileSizeMB / this.megabytesPerSecond);

    

    VideoQuality(double bitrateMBps) {
        this.bitrateMBps = bitrateMBps;
    }

    public double getBitrateMBps() {
        return bitrateMBps;

    }
}
