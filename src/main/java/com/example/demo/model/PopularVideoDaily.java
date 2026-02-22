package com.example.demo.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "popular_videos_daily")
public class PopularVideoDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition; // 1, 2, 3

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "popularity_score", nullable = false)
    private Double popularityScore;

    @Column(name = "views_day_1", nullable = false) // prethodni dan (težina 7)
    private Long viewsDay1;

    @Column(name = "views_day_2", nullable = false) // 2 dana unazad (težina 6)
    private Long viewsDay2;

    @Column(name = "views_day_3", nullable = false)
    private Long viewsDay3;

    @Column(name = "views_day_4", nullable = false)
    private Long viewsDay4;

    @Column(name = "views_day_5", nullable = false)
    private Long viewsDay5;

    @Column(name = "views_day_6", nullable = false)
    private Long viewsDay6;

    @Column(name = "views_day_7", nullable = false) // 7 dana unazad (težina 1)
    private Long viewsDay7;

    // Constructors
    public PopularVideoDaily() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getCalculationDate() { return calculationDate; }
    public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }

    public Long getViewsDay1() { return viewsDay1; }
    public void setViewsDay1(Long viewsDay1) { this.viewsDay1 = viewsDay1; }

    public Long getViewsDay2() { return viewsDay2; }
    public void setViewsDay2(Long viewsDay2) { this.viewsDay2 = viewsDay2; }

    public Long getViewsDay3() { return viewsDay3; }
    public void setViewsDay3(Long viewsDay3) { this.viewsDay3 = viewsDay3; }

    public Long getViewsDay4() { return viewsDay4; }
    public void setViewsDay4(Long viewsDay4) { this.viewsDay4 = viewsDay4; }

    public Long getViewsDay5() { return viewsDay5; }
    public void setViewsDay5(Long viewsDay5) { this.viewsDay5 = viewsDay5; }

    public Long getViewsDay6() { return viewsDay6; }
    public void setViewsDay6(Long viewsDay6) { this.viewsDay6 = viewsDay6; }

    public Long getViewsDay7() { return viewsDay7; }
    public void setViewsDay7(Long viewsDay7) { this.viewsDay7 = viewsDay7; }
}