package com.example.demo.dtos;

public class ScheduledStreamResponse {
    private Long videoId;
    private String title;
    private boolean isAvailable;
    private String scheduledAt; // ISO DateTime string
    private long durationSeconds; // Trajanje videa u sekundama
    private Integer currentOffsetSeconds; // Trenutni offset (ako je dostupan)
    private String streamStatus; // "NOT_STARTED", "LIVE", "FINISHED", "NOT_SCHEDULED"
    private String message;

    public ScheduledStreamResponse() {}

    public ScheduledStreamResponse(Long videoId, String title, boolean isAvailable,
                                  String scheduledAt, long durationSeconds,
                                  Integer currentOffsetSeconds, String streamStatus,
                                  String message) {
        this.videoId = videoId;
        this.title = title;
        this.isAvailable = isAvailable;
        this.scheduledAt = scheduledAt;
        this.durationSeconds = durationSeconds;
        this.currentOffsetSeconds = currentOffsetSeconds;
        this.streamStatus = streamStatus;
        this.message = message;
    }

    // Getters and Setters
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getCurrentOffsetSeconds() { return currentOffsetSeconds; }
    public void setCurrentOffsetSeconds(Integer currentOffsetSeconds) { this.currentOffsetSeconds = currentOffsetSeconds; }

    public String getStreamStatus() { return streamStatus; }
    public void setStreamStatus(String streamStatus) { this.streamStatus = streamStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
