package com.example.demo.service.impl;

import com.example.demo.dtos.CreateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.User;
import com.example.demo.model.Video;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VideoRepository;
import com.example.demo.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Value("${app.storage.base-dir:./uploads}")
    private String baseDir;

    @Value("${app.storage.upload-timeout-seconds:15}")
    private int uploadTimeoutSeconds;

    public VideoServiceImpl(VideoRepository videoRepository, UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public VideoDTO create(CreateVideoDTO data, MultipartFile thumbnail, MultipartFile video, String authEmail) {

        User author = userRepository.findByEmail(authEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Path thumbDir = Path.of(baseDir+"/thumbnails");
        Path videoDir = Path.of(baseDir+"/videos");

        try {
            Files.createDirectories(thumbDir);
            Files.createDirectories(videoDir);

            String thumbName = UUID.randomUUID() + "-" + thumbnail.getOriginalFilename();
            String videoName = UUID.randomUUID() + "-" + video.getOriginalFilename();

            Path thumbPath = thumbDir.resolve(thumbName);
            Path videoPath = videoDir.resolve(videoName);

            thumbnail.transferTo(thumbPath);
            video.transferTo(videoPath);

            Video v = new Video();
            v.setAuthor(author);
            v.setTitle(data.getTitle());
            v.setDescription(data.getDescription());
            v.setTags(data.getTags());
            v.setGeoLocation(data.getLocation());
            v.setCreatedAt(Instant.now());

            v.setThumbnailPath(thumbPath.toString());
            v.setVideoPath(videoPath.toString());

            Video saved = videoRepository.save(v);

            System.out.println("SAVED video id=" + saved.getId()
                    + " thumb=" + saved.getThumbnailPath()
                    + " video=" + saved.getVideoPath());

            return toDto(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store files", e);
        }
    }


    @Override
    public List<VideoDTO> listNewestFirst() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).toList();
    }

    @Override
    public VideoDTO getById(Long id) {
        Video v = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return toDto(v);
    }

    @Override
    public Resource getVideoResource(Long id) {
        Video v = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Path p = Path.of(v.getVideoPath());
        if (!Files.exists(p)) throw new IllegalArgumentException("Video file missing on disk");

        return new FileSystemResource(p);
    }

    @Override
    @Cacheable(cacheNames = "videoThumbnails", key = "#id")
    public ThumbnailPayload loadThumbnail(Long id) throws IOException {
        Video v = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Path p = Path.of(v.getThumbnailPath());
        if (!Files.exists(p)) throw new IllegalArgumentException("Thumbnail missing on disk");

        byte[] bytes = Files.readAllBytes(p);
        String contentType = Files.probeContentType(p);
        if (contentType == null) contentType = MediaType.IMAGE_JPEG_VALUE;

        return new ThumbnailPayload(bytes, contentType);
    }


    private void validateCreate(CreateVideoDTO data, MultipartFile thumbnail, MultipartFile video) {
        if (data == null) throw new IllegalArgumentException("Missing data part");
        if (isBlank(data.getTitle())) throw new IllegalArgumentException("Title is required");
        if (isBlank(data.getDescription())) throw new IllegalArgumentException("Description is required");
        if (data.getTags() == null) throw new IllegalArgumentException("Tags are required");

        if (thumbnail == null || thumbnail.isEmpty()) throw new IllegalArgumentException("Thumbnail is required");
        String thumbCt = thumbnail.getContentType() == null ? "" : thumbnail.getContentType();
        if (!thumbCt.startsWith("image/")) throw new IllegalArgumentException("Thumbnail must be an image");

        if (video == null || video.isEmpty()) throw new IllegalArgumentException("Video is required");
        String videoCt = video.getContentType() == null ? "" : video.getContentType();
        if (!"video/mp4".equalsIgnoreCase(videoCt)) {
        }
        long max = 200L * 1024 * 1024;
        if (video.getSize() > max) throw new IllegalArgumentException("Video max size is 200MB");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public long registerView(Long id) {
        int updated = videoRepository.incrementViewCount(id);
        if (updated == 0) throw new IllegalArgumentException("Video not found");
        return videoRepository.getViewCount(id);
    }

    private void saveWithTimeout(MultipartFile file, Path dest, int timeoutSeconds) throws Exception {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            f.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            f.cancel(true);
            throw te;
        } finally {
            ex.shutdownNow();
        }
    }

    private void safeDelete(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String guessImageExt(MultipartFile thumbnail) {
        String ct = thumbnail.getContentType();
        if (ct == null) return ".jpg";
        return switch (ct) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".jpg";
        };
    }

    private VideoDTO toDto(Video v) {
        VideoDTO dto = new VideoDTO();
        dto.setId(v.getId());
        dto.setTitle(v.getTitle());
        dto.setDescription(v.getDescription());
        dto.setTags(v.getTags());
        dto.setLocation(v.getGeoLocation());
        dto.setViewCount(v.getViewCount());
        return dto;
    }
}
