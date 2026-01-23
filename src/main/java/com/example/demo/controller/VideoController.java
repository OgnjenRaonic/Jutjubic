package com.example.demo.controller;

import com.example.demo.dtos.CreateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import com.example.demo.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB chunk (ok za premotavanje)

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Long> registerView(@PathVariable Long id) {
        long newCount = videoService.registerView(id);
        return ResponseEntity.ok(newCount);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VideoDTO create(
            @ModelAttribute CreateVideoDTO data,
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
}
