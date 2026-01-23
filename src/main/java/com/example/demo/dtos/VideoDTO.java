package com.example.demo.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class VideoDTO {

    private Long id;
    private String title;
    private String description;
    private List<String> tags;
    private LocalDateTime createdAt;
    private String location;      // opciono (može null)
    private String ownerEmail;    // ko je postavio (auth.getName())
    private long viewCount;
    private LocalDateTime scheduledAt; // za zakazane videe
    private boolean available;    // da li je dostupan sada
    private Integer currentOffsetSeconds; // trenutni offset u streaming modu

    public VideoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Integer getCurrentOffsetSeconds() { return currentOffsetSeconds; }
    public void setCurrentOffsetSeconds(Integer currentOffsetSeconds) { this.currentOffsetSeconds = currentOffsetSeconds; }
}
