package com.example.demo.service;

import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.GeoPoint;
import java.util.List;

public interface TrendingService {
    List<VideoDTO> findLocalTrending(GeoPoint center, double radiusKm);
}
