package com.example.demo.service.impl;

import com.example.demo.dtos.CreateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.User;
import com.example.demo.model.Video;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VideoRepository;
import com.example.demo.service.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoServiceImpl(VideoRepository videoRepository, UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public VideoDTO create(CreateVideoDTO data, MultipartFile thumbnail, MultipartFile videoFile, String username) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nedostaju podaci.");
        if (isBlank(data.getTitle())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Naslov je obavezan.");
        if (isBlank(data.getDescription())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opis je obavezan.");
        if (data.getTags() == null || data.getTags().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tagovi su obavezni.");
        if (thumbnail == null || thumbnail.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thumbnail je obavezan.");
        if (videoFile == null || videoFile.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video je obavezan.");

        // basic checks
        if (!"video/mp4".equals(videoFile.getContentType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video mora biti mp4.");
        long maxBytes = 200L * 1024L * 1024L;
        if (videoFile.getSize() > maxBytes)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video je veći od 200MB.");

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Save files locally (simple, no rollback here)
        String thumbPath = saveToUploads(thumbnail, "thumbnails");
        String vidPath = saveToUploads(videoFile, "videos");

        Video v = new Video();
        v.setAuthor(author);
        v.setTitle(data.getTitle().trim());
        v.setDescription(data.getDescription().trim());
        v.setTags(normalizeTags(data.getTags()));
        v.setCreatedAt(Instant.now());
        v.setGeoLocation(blankToNull(data.getGeoLocation()));
        v.setThumbnailPath(thumbPath);
        v.setVideoPath(vidPath);

        Video saved = videoRepository.save(v);

        return VideoDTO.from(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getTags(),
                saved.getCreatedAt(),
                saved.getGeoLocation(),
                saved.getAuthor().getId(),
                saved.getAuthor().getUsername()
        );
    }

    private String saveToUploads(MultipartFile file, String folder) {
        try {
            Path dir = Paths.get("./uploads", folder);
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.') + 1)
                    : "bin";

            String name = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(name);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Neuspešan upload fajla.");
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase())
                .distinct()
                .toList();
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
