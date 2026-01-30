package com.example.demo.service.impl;

import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.GeoPoint;
import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.VideoViewRepository;
import com.example.demo.service.TrendingService;
import com.example.demo.service.VideoService;
import com.example.demo.util.GeohashUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrendingServiceImpl implements TrendingService {

    private final VideoViewRepository videoViewRepository;
    private final VideoRepository videoRepository;
    private final VideoService videoService;

    @Value("${trending.windowHours:24}")
    private int windowHours;

    @Value("${trending.limit:20}")
    private int limit;

    @Value("${trending.geohashPrecision:5}")
    private int geohashPrecision;

    public TrendingServiceImpl(VideoViewRepository videoViewRepository, VideoRepository videoRepository, VideoService videoService) {
        this.videoViewRepository = videoViewRepository;
        this.videoRepository = videoRepository;
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

        List<Video> allVideos = videoRepository.findAll();
        Map<Long, Video> videoById = new HashMap<>();
        for (Video v : allVideos) {
            videoById.put(v.getId(), v);
        }

        class VideoWithScore {
            VideoDTO dto;
            double score;
            VideoWithScore(VideoDTO dto, double score) {
                this.dto = dto;
                this.score = score;
            }
        }

        List<VideoWithScore> scored = new ArrayList<>();
        for (Object[] row : rows) {
            Long videoId = ((Number) row[0]).longValue();
            long localViews = ((Number) row[1]).longValue();

            Video video = videoById.get(videoId);
            if (video == null) continue;

            double score = calculateTrendingScoreLocal(video, localViews, allVideos);
            VideoDTO dto = videoService.getById(videoId);
            dto.setTrendingScore(score);
            scored.add(new VideoWithScore(dto, score));
        }

        scored.sort(Comparator.comparingDouble((VideoWithScore v) -> v.score).reversed());

        List<VideoDTO> result = new ArrayList<>();
        for (VideoWithScore v : scored) {
            if (result.size() >= max) break;
            result.add(v.dto);
        }

        return result;
    }

    private double calculateTrendingScoreLocal(Video video, long localViews, List<Video> allVideos) {
        Instant createdAt = video.getCreatedAt();
        long now = System.currentTimeMillis();
        long videoAgeMs = createdAt == null ? 0 : Math.max(0, now - createdAt.toEpochMilli());
        double ageInDays = videoAgeMs / (1000.0 * 60 * 60 * 24);

        double viewScore = Math.log1p(localViews) / Math.log1p(100);
        viewScore = Math.min(1.0, viewScore);

        double commentScore = Math.log1p(video.getCommentCount()) / Math.log1p(50);
        commentScore = Math.min(1.0, commentScore);

        double recencyScore = 1.0 / (1.0 + Math.log1p(ageInDays));
        recencyScore = Math.min(1.0, recencyScore);

        double tagScore = calculateTagPopularityScore(video, allVideos);

        return (viewScore * 60)
            + (commentScore * 20)
            + (recencyScore * 10)
            + (tagScore * 10);
    }

    private double calculateTagPopularityScore(Video video, List<Video> allVideos) {
        if (video.getTags() == null || video.getTags().isEmpty()) {
            return 0.0;
        }

        double maxPopularity = 0;
        for (String tag : video.getTags()) {
            long tagCount = allVideos.stream()
                .filter(v -> !v.getId().equals(video.getId()))
                .filter(v -> v.getTags() != null && v.getTags().contains(tag))
                .count();

            double tagPopularity = Math.min(1.0, tagCount / 50.0);
            maxPopularity = Math.max(maxPopularity, tagPopularity);
        }
        return maxPopularity;
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
