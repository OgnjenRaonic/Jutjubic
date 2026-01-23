package com.example.demo.dtos;

public class RateLimitInfoDTO {
    private int commentsInLastHour;
    private boolean canComment;
    private String nextAvailableAt; // ISO string, nullable

    public RateLimitInfoDTO(int commentsInLastHour, boolean canComment, String nextAvailableAt) {
        this.commentsInLastHour = commentsInLastHour;
        this.canComment = canComment;
        this.nextAvailableAt = nextAvailableAt;
    }

    public int getCommentsInLastHour() { return commentsInLastHour; }
    public boolean isCanComment() { return canComment; }
    public String getNextAvailableAt() { return nextAvailableAt; }
}
