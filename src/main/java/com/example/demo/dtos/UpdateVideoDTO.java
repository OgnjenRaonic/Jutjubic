package com.example.demo.dtos;

import java.util.List;

public class UpdateVideoDTO {
    private String title;
    private String description;
    private List<String> tags;
    private String geoLocation;
    private String scheduledAt; // ISO DateTime string - nullable

    public UpdateVideoDTO() {}

    public UpdateVideoDTO(String title, String description, List<String> tags, 
                         String geoLocation, String scheduledAt) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.geoLocation = geoLocation;
        this.scheduledAt = scheduledAt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getGeoLocation() { return geoLocation; }
    public void setGeoLocation(String geoLocation) { this.geoLocation = geoLocation; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }
}
