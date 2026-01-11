package com.example.demo.dtos;

import java.util.List;

public class CreateVideoDTO {

    private String title;
    private String description;
    private List<String> tags;
    private String location; // opcionalno

    public CreateVideoDTO() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
