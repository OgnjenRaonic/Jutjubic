package com.example.demo.model;

public enum VideoQuality {
    LOW(0.6),        // 360p, niska kvaliteta - ~0.6 MB/s
    MEDIUM(1.2),     // 720p, srednja kvaliteta - ~1.2 MB/s  
    HIGH(2.0);       // 1080p+, visoka kvaliteta - ~2.0 MB/s

    private final double bitrateMBps;

    VideoQuality(double bitrateMBps) {
        this.bitrateMBps = bitrateMBps;
    }

    public double getBitrateMBps() {
        return bitrateMBps;
    }
}
