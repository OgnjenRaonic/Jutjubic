package com.example.demo.dtos;

public class CommentDTO {
    private Long id;
    private Long videoId;
    private Long userId;
    private String username;
    private String text;
    private String createdAt; // ISO string

    public CommentDTO(Long id, Long videoId, Long userId, String username, String text, String createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getVideoId() { return videoId; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }
}
