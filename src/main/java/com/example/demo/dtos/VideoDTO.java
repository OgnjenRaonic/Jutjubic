package com.example.demo.dtos;

import java.time.Instant;
import java.util.List;

public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private List<String> tags;
    private Instant createdAt;
    private String geoLocation;

    private Long authorId;
    private String authorUsername;

    private String thumbnailUrl;
    private String videoUrl;

    public static VideoDTO from(Long id, String title, String description, List<String> tags,
                                Instant createdAt, String geoLocation,
                                Long authorId, String authorUsername) {
        VideoDTO dto = new VideoDTO();
        dto.id = id;
        dto.title = title;
        dto.description = description;
        dto.tags = tags;
        dto.createdAt = createdAt;
        dto.geoLocation = geoLocation;
        dto.authorId = authorId;
        dto.authorUsername = authorUsername;
        dto.thumbnailUrl = "/api/videos/" + id + "/thumbnail";
        dto.videoUrl = "/api/videos/" + id + "/video";
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public String getGeoLocation() { return geoLocation; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getVideoUrl() { return videoUrl; }
}
