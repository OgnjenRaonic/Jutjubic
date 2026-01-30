package com.example.demo.service;

import com.example.demo.model.GeoPoint;
import com.example.demo.model.Video;
import com.example.demo.model.VideoView;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.VideoViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ViewService  {

    private final VideoRepository videoRepository;
    private final VideoViewRepository videoViewRepository;
    private final GeoIpService geoIpService;

    // ~1.11km po 0.01 deg latitude (grubo). Možeš menjati u application.properties
    @Value("${trending.cellSizeDeg:0.01}")
    private double cellSizeDeg;

    public ViewService(VideoRepository videoRepository, VideoViewRepository videoViewRepository, GeoIpService geoIpService) {
        this.videoRepository = videoRepository;
        this.videoViewRepository = videoViewRepository;
        this.geoIpService = geoIpService;
    }

    public void registerView(Long videoId, Double lat, Double lon, HttpServletRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        GeoPoint p = (lat != null && lon != null)
                ? new GeoPoint(lat, lon, "GPS")
                : geoIpService.approximateFromRequest(request); // "IP" ili "DEFAULT_DEV"

        int cellLat = (int) Math.floor(p.lat() / cellSizeDeg);
        int cellLon = (int) Math.floor(p.lon() / cellSizeDeg);

        videoViewRepository.save(new VideoView(video, p.lat(), p.lon(), p.source(), cellLat, cellLon));

        // zadrži postojeći globalni counter
        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);
    }
}
