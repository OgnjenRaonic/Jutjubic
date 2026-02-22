package com.example.demo.dtos;

public class PopularVideoDTO {
    private Long videoId;
    private String title;
    private String thumbnailUrl;
    private Integer rankPosition;
    private Double popularityScore;
    private Long totalViews7Days;

    // Constructors
    public PopularVideoDTO() {}

    public PopularVideoDTO(Long videoId, String title, String thumbnailUrl,
                           Integer rankPosition, Double popularityScore, Long totalViews7Days) {
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.rankPosition = rankPosition;
        this.popularityScore = popularityScore;
        this.totalViews7Days = totalViews7Days;
    }

    // Getters and Setters
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }

    public Long getTotalViews7Days() { return totalViews7Days; }
    public void setTotalViews7Days(Long totalViews7Days) { this.totalViews7Days = totalViews7Days; }
}