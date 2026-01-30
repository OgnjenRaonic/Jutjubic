package com.example.demo.controller;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.demo.service.ViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dtos.CreateVideoDTO;

import com.example.demo.dtos.ScheduledStreamResponse;
import com.example.demo.dtos.UpdateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import com.example.demo.service.ScheduledStreamingService;
import com.example.demo.service.VideoService;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB chunk (ok za premotavanje)

    private final VideoService videoService;
    private final ScheduledStreamingService scheduledStreamingService;
    private final ViewService viewService;

    public VideoController(VideoService videoService, ViewService viewService, ScheduledStreamingService scheduledStreamingService) {
        this.videoService = videoService;
        this.scheduledStreamingService = scheduledStreamingService;
        this.viewService = viewService;
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> registerView(
            @PathVariable Long id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            HttpServletRequest request
    ) {
        viewService.registerView(id, lat, lon, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VideoDTO create(
            @RequestPart("data") CreateVideoDTO data,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("video") MultipartFile video,
            Authentication auth
    ) {
        return videoService.create(data, thumbnail, video, auth.getName());
    }

    @GetMapping
    public List<VideoDTO> list() {
        return videoService.listNewestFirst();
    }

    @GetMapping("/trending")
    public ResponseEntity<List<VideoDTO>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        List<VideoDTO> trending = videoService.getTrendingVideos(Math.min(limit, 100)); // Max 100
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/{id}")
    public VideoDTO get(@PathVariable Long id) {
        return videoService.getById(id);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long id) throws IOException {
        var thumb = videoService.loadThumbnail(id);
        MediaType ct = MediaType.parseMediaType(thumb.contentType());

        return ResponseEntity.ok()
                .contentType(ct)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(thumb.bytes());
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<ResourceRegion> stream(
            @PathVariable Long id,
            @RequestHeader HttpHeaders headers
    ) throws IOException {

        Resource video = videoService.getVideoResource(id);
        long contentLength = video.contentLength();

        List<HttpRange> ranges = headers.getRange();
        ResourceRegion region;

        if (ranges == null || ranges.isEmpty()) {
            long rangeLength = Math.min(CHUNK_SIZE, contentLength);
            region = new ResourceRegion(video, 0, rangeLength);

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.valueOf("video/mp4")))
                    .contentLength(rangeLength)
                    .body(region);
        } else {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);

            long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
            region = new ResourceRegion(video, start, rangeLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.valueOf("video/mp4")))
                    .body(region);
        }
    }

    /**
     * GET /api/videos/{id}/scheduled-info
     * Dobija informacije o zakazanom videu i trenutnom streaming offsetu
     */
    @GetMapping("/{id}/scheduled-info")
    public ResponseEntity<?> getScheduledInfo(@PathVariable Long id) {
        try {
            ScheduledStreamResponse info = scheduledStreamingService.getScheduledStreamInfo(id);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Greška pri čitanju zakazane informacije: " + e.getMessage());
        }
    }

    /**
     * PUT /api/videos/{id}
     * Ažurira video informacije uključujući zakazano vreme
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(
            @PathVariable Long id,
            @RequestBody UpdateVideoDTO dto,
            Authentication auth) {
        try {
            // Privremeno - trebalo bi da se prosledi ID vlasnika
            if (auth == null) {
                return ResponseEntity.status(401).body("Morate biti prijavljeni");
            }

            // Ažuriraj zakazano vreme ako je dostavljeno
            if (dto.getScheduledAt() != null && !dto.getScheduledAt().isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                LocalDateTime scheduledAt;

                // Pokušaj da parsiram sa vremenskom zonom
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dto.getScheduledAt(), formatter);
                    // Ako ima zone, konvertuj u CET
                    scheduledAt = zonedDateTime.withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime();
                } catch (DateTimeParseException e) {
                    // Ako nema zone u stringu, parsniraj kao LocalDateTime
                    DateTimeFormatter localeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                    LocalDateTime localDT = LocalDateTime.parse(dto.getScheduledAt(), localeFormatter);
                    // VAŽNO: Pretpostavi da je ovo VEĆ CET vreme (jer dolazi sa frontenда koji je u CET)
                    // Ne trebamo da konvertujemo, samo da preuzmemo kao što je
                    scheduledAt = localDT;
                }

                scheduledStreamingService.scheduleVideo(id, scheduledAt);
            } else if (dto.getScheduledAt() != null && dto.getScheduledAt().isEmpty()) {
                // Ako je prazan string, otkaži zakazivanje
                scheduledStreamingService.unscheduleVideo(id);
            }

            // Dobij ažurirani video
            VideoDTO updated = videoService.getById(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Greška pri ažuriranju videa: " + e.getMessage());
        }
    }}