package com.example.demo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "video_views",
        indexes = {
                @Index(name = "idx_views_time", columnList = "viewed_at"),
                @Index(name = "idx_views_cell_time", columnList = "cell_lat,cell_lon,viewed_at"),
                @Index(name = "idx_views_video_time", columnList = "video_id,viewed_at"),
                @Index(name = "idx_views_geohash_time", columnList = "geohash,viewed_at")
        }
)
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private Instant viewedAt;

    @Column(name="geohash", nullable=false, length=12)
    private String geohash;

    @Column(name = "viewer_lat", nullable = false)
    private Double viewerLat;

    @Column(name = "viewer_lon", nullable = false)
    private Double viewerLon;

    @Column(name = "source", nullable = false, length = 20)
    private String source; // "GPS" ili "IP" (ili "DEFAULT_DEV")

    // “prostorno indeksiranje” bez PostGIS-a: grid ćelije + indeks
    @Column(name = "cell_lat", nullable = false)
    private Integer cellLat;

    @Column(name = "cell_lon", nullable = false)
    private Integer cellLon;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) viewedAt = Instant.now();
    }

    public VideoView() {}

    public VideoView(Video video, double lat, double lon, String source, int cellLat, int cellLon, String geohash) {
        this.video = video;
        this.viewerLat = lat;
        this.viewerLon = lon;
        this.source = source;
        this.cellLat = cellLat;
        this.cellLon = cellLon;
        this.geohash = geohash;
    }
    public Instant getViewedAt() {
        return viewedAt;
    }

    public Video getVideo() {
        return video;
    }

    public Long getId() {
        return id;
    }

    public String getGeohash() {
        return geohash;
    }

    public Double getViewerLat() {
        return viewerLat;
    }

    public Double getViewerLon() {
        return viewerLon;
    }

    public String getSource() {
        return source;
    }

    public Integer getCellLat() {
        return cellLat;
    }

    public Integer getCellLon() {
        return cellLon;
    }
}
