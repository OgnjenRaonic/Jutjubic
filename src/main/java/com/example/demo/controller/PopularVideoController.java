package com.example.demo.controller;

import com.example.demo.dtos.PopularVideoDTO;
import com.example.demo.service.EtlPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/popular")
public class PopularVideoController {

    @Autowired
    private EtlPipelineService etlPipelineService;

    /**
     * GET /api/popular/daily
     * Vraća top 3 popularna videa za danas (ili poslednji izračunati dan)
     */
    @GetMapping("/daily")
    public ResponseEntity<List<PopularVideoDTO>> getDailyPopularVideos() {
        List<PopularVideoDTO> top3 = etlPipelineService.getTop3PopularVideos();
        return ResponseEntity.ok(top3);
    }
    @GetMapping("/run-etl")
    public ResponseEntity<String> runEtlManually() {
        etlPipelineService.runDailyEtlPipeline();
        return ResponseEntity.ok("ETL pipeline pokrenut ručno");
    }
}