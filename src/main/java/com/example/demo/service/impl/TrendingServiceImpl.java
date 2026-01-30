package com.example.demo.service.impl;

import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.GeoPoint;
import com.example.demo.repository.VideoViewRepository;
import com.example.demo.service.TrendingService;
import com.example.demo.service.VideoService;
import com.example.demo.util.GeohashUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrendingServiceImpl implements TrendingService {

    private final VideoViewRepository videoViewRepository;
    private final VideoService videoService;

    @Value("${trending.windowHours:24}")
    private int windowHours;

    @Value("${trending.limit:20}")
    private int limit;

    @Value("${trending.geohashPrecision:5}")
    private int geohashPrecision;

    public TrendingServiceImpl(VideoViewRepository videoViewRepository, VideoService videoService) {
        this.videoViewRepository = videoViewRepository;
        this.videoService = videoService;
    }

    @Override
    public List<VideoDTO> findLocalTrending(GeoPoint center, double radiusKm) {
        if (center == null || radiusKm <= 0) return List.of();

        int precision = geohashPrecision > 0 ? geohashPrecision : GeohashUtil.precisionForRadiusKm(radiusKm);
        List<String> hashes = coverHashes(center, radiusKm, precision);
        if (hashes.isEmpty()) return List.of();

        int hours = windowHours > 0 ? windowHours : 24;
        Instant since = Instant.now().minus(Duration.ofHours(hours));

        List<Object[]> rows = videoViewRepository.topVideoIdsInHashesSince(since, hashes);
        int max = Math.min(limit > 0 ? limit : 20, 100);

        List<VideoDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= max) break;
            Long videoId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();

            VideoDTO dto = videoService.getById(videoId);
            dto.setTrendingScore((double) count);
            result.add(dto);
        }
        return result;
    }

    private List<String> coverHashes(GeoPoint center, double radiusKm, int precision) {
        double lat = center.getLat();
        double lon = center.getLon();

        double dLat = radiusKm / 111.32;
        double cosLat = Math.cos(Math.toRadians(lat));
        double kmPerDegLon = 111.32 * Math.max(0.01, Math.abs(cosLat));
        double dLon = radiusKm / kmPerDegLon;

        double minLat = clamp(lat - dLat, -90, 90);
        double maxLat = clamp(lat + dLat, -90, 90);
        double minLon = clamp(lon - dLon, -180, 180);
        double maxLon = clamp(lon + dLon, -180, 180);

        String centerHash = GeohashUtil.encode(lat, lon, precision);
        GeohashUtil.BBox bbox = GeohashUtil.decodeBBox(centerHash);
        double latStep = Math.max(0.0001, bbox.maxLat() - bbox.minLat());
        double lonStep = Math.max(0.0001, bbox.maxLon() - bbox.minLon());

        Set<String> hashes = new LinkedHashSet<>();
        for (double y = minLat; y <= maxLat + 1e-9; y += latStep) {
            for (double x = minLon; x <= maxLon + 1e-9; x += lonStep) {
                hashes.add(GeohashUtil.encode(y, x, precision));
            }
        }
        return new ArrayList<>(hashes);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
