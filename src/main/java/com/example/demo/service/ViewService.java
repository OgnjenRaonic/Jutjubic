package com.example.demo.service;

import com.example.demo.model.GeoPoint;
import com.example.demo.model.Video;
import com.example.demo.model.VideoView;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.VideoViewRepository;
import com.example.demo.util.GeohashUtil;
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

    @Value("${trending.geohashPrecision:5}")
    private int geohashPrecision;

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
                : geoIpService.approximateFromRequest(request);

        if (p == null
                || Double.isNaN(p.getLat()) || Double.isNaN(p.getLon())
                || p.getLat() < -90 || p.getLat() > 90
                || p.getLon() < -180 || p.getLon() > 180) {
            p = new GeoPoint(45.2671, 19.8335, "DEFAULT_DEV");
        }

        double cs = (cellSizeDeg > 0) ? cellSizeDeg : 0.01;
        int cellLat = (int) Math.floor(p.getLat() / cs);
        int cellLon = (int) Math.floor(p.getLon() / cs);

        int precision = geohashPrecision > 0 ? geohashPrecision : GeohashUtil.precisionForRadiusKm(10);
        String geohash = GeohashUtil.encode(p.getLat(), p.getLon(), precision);

        videoViewRepository.save(new VideoView(video, p.getLat(), p.getLon(), p.getSource(), cellLat, cellLon, geohash));

        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);
    }


}
