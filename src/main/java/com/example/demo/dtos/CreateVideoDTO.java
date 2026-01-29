package com.example.demo.dtos;

import java.util.List;

import com.example.demo.model.VideoQuality;

public class CreateVideoDTO {

    private String title;
    private String description;
    private List<String> tags;
    private String location; // opcionalno
    private VideoQuality quality = VideoQuality.MEDIUM; // default MEDIUM

    public CreateVideoDTO() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public VideoQuality getQuality() { return quality; }
    public void setQuality(VideoQuality quality) { this.quality = quality; }
}
