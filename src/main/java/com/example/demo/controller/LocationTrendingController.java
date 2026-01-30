package com.example.demo.controller;

import com.example.demo.model.GeoPoint;
import com.example.demo.service.GeoIpService;
import com.example.demo.service.TrendingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LocationTrendingController {

    private final GeoIpService geoIpService;
    private final TrendingService trendingService;

    public LocationTrendingController(GeoIpService geoIpService, TrendingService trendingService) {
        this.geoIpService = geoIpService;
        this.trendingService = trendingService;
    }

    @GetMapping("/api/trending/local")
    public ResponseEntity<?> localTrending(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "10") double radiusKm,
            HttpServletRequest request
    ) {
        GeoPoint center = (lat != null && lon != null)
                ? new GeoPoint(lat, lon, "GPS")
                : geoIpService.approximateFromRequest(request);

        return ResponseEntity.ok(trendingService.findLocalTrending(center, radiusKm));
    }
}
