package com.example.demo.service;

import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;
import com.example.demo.dtos.ScheduledStreamResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;

@Service
public class ScheduledStreamingService {
    @Autowired
    private VideoRepository videoRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final long DEFAULT_DURATION_SECONDS = 600; // 10 minuta po defaultu

    /**
     * Proverava da li je video dostupan za pregled
     */
    @Transactional(readOnly = true)
    public boolean isVideoAvailable(Long videoId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (video.getScheduledAt() == null) {
            return true; // Nema zakazanog vremena, video je javno dostupan
        }

        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(video.getScheduledAt()); // Dostupan ako je sada >= zakazanog vremena
    }

    /**
     * Dobija trenutni offset u streaming-u (sekunde od početka)
     * Koristi se za synchronized streaming - svi koji gledaju vidim se nalaze na istoj minutaži
     */
    @Transactional(readOnly = true)
    public Integer getCurrentStreamOffset(Long videoId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (video.getScheduledAt() == null) {
            return null; // Nema zakazanog vremena, nije streaming
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(video.getScheduledAt())) {
            return null; // Streaming još nije počeo
        }

        // Offset = razlika između sada i zakazanog vremena
        Duration duration = Duration.between(video.getScheduledAt(), now);
        long offsetSeconds = duration.getSeconds();

        // Dobij trajanje videa
        long videoDuration = getVideoDurationSeconds(video);

        // Ako je offset veći od trajanja, video je završen
        if (offsetSeconds >= videoDuration) {
            return (int) videoDuration;
        }

        return (int) offsetSeconds;
    }

    /**
     * Dobija kompletne informacije o zakazanom videu
     */
    @Transactional(readOnly = true)
    public ScheduledStreamResponse getScheduledStreamInfo(Long videoId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (video.getScheduledAt() == null) {
            throw new IllegalArgumentException("Video nije zakazan");
        }

        boolean isAvailable = isVideoAvailable(videoId);
        Integer offset = getCurrentStreamOffset(videoId);
        long duration = getVideoDurationSeconds(video);
        LocalDateTime scheduledAt = video.getScheduledAt();
        LocalDateTime now = LocalDateTime.now();

        String streamStatus;
        String message;

        if (now.isBefore(scheduledAt)) {
            streamStatus = "NOT_STARTED";
            Duration timeUntilStart = Duration.between(now, scheduledAt);
            message = "Video počinje za " + formatDuration(timeUntilStart);
        } else if (offset >= duration) {
            streamStatus = "FINISHED";
            message = "Streaming je završen";
        } else {
            streamStatus = "LIVE";
            message = "Streaming je u toku";
        }

        return new ScheduledStreamResponse(
            videoId,
            video.getTitle(),
            isAvailable,
            scheduledAt.format(ISO_FORMATTER),
            duration,
            offset,
            streamStatus,
            message
        );
    }

    /**
     * Pronalazi trajanje videa u sekundama analizirajući video fajl
     * Za sada koristi default vrednost ili osnovnu procenu
     */
    private long getVideoDurationSeconds(Video video) {
        if (video.getVideoPath() == null || video.getVideoPath().isEmpty()) {
            return DEFAULT_DURATION_SECONDS;
        }

        try {
            File videoFile = new File(video.getVideoPath());
            if (!videoFile.exists()) {
                return DEFAULT_DURATION_SECONDS;
            }

            // Pokušaj da koristi ffprobe da dobije trajanje (ako je dostupno)
            // Za sada, koristi heurističku procenu po veličini fajla
            // ~1MB = ~1 sekunda za prosečan video
            long fileSizeBytes = videoFile.length();
            long estimatedSeconds = Math.max(1, fileSizeBytes / (1024 * 1024)); // 1MB = 1s
            
            // Ograniči na razumnu vrednost (max 1 sat)
            return Math.min(estimatedSeconds, 3600);
        } catch (Exception e) {
            return DEFAULT_DURATION_SECONDS;
        }
    }

    /**
     * Formatira Duration u čitljiv string
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0) {
            return String.format("%d h %d m", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d m %d s", minutes, seconds);
        } else {
            return String.format("%d s", seconds);
        }
    }

    /**
     * Ažurira zakazano vreme za video
     */
    @Transactional
    public void scheduleVideo(Long videoId, LocalDateTime scheduledAt) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (scheduledAt != null && scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Zakazano vreme ne može biti u prošlosti");
        }

        video.setScheduledAt(scheduledAt);
        videoRepository.save(video);
    }

    /**
     * Otkazuje zakazivanje videa (čini ga odmah dostupnim)
     */
    @Transactional
    public void unscheduleVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
            .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        video.setScheduledAt(null);
        videoRepository.save(video);
    }
}
