package com.example.demo.dtos;

public class ChatMessage {
    private Long videoId;
    private String sender;
    private String content;
    private String sentAt;

    public ChatMessage() {}

    public ChatMessage(Long videoId, String sender, String content, String sentAt) {
        this.videoId = videoId;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
}
