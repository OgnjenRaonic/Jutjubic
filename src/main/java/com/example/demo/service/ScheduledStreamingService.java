package com.example.demo.service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.example.demo.dtos.ScheduledStreamResponse;
import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;

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

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));
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

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));
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
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));

        System.out.println("[STREAM INFO] Video #" + videoId);
        System.out.println("[STREAM INFO] Sada: " + now);
        System.out.println("[STREAM INFO] Zakazano: " + scheduledAt);
        System.out.println("[STREAM INFO] Trajanje: " + duration + "s");
        System.out.println("[STREAM INFO] Offset: " + offset + "s");
        System.out.println("[STREAM INFO] Dostupan: " + isAvailable);

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
        
        System.out.println("[STREAM INFO] Status: " + streamStatus);

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
     * Koristi kvalitet videa (LOW, MEDIUM, HIGH) za adaptivnu procenu
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
            // Prvo pokušaj da dobiješ trajanje iz MP4 metapodataka
            Long metaSeconds = getMp4DurationSeconds(videoFile);
            if (metaSeconds != null && metaSeconds > 0) {
                long bounded = Math.min(Math.max(metaSeconds, 10L), 7200L);
                System.out.println("[DURATION] Metadata duration: " + metaSeconds + "s, final: " + bounded + "s");
                return bounded;
            }

            // Ako metadata nije dostupna, koristi adaptivnu heuristiku na osnovu kvaliteta videa
            // LOW (360p): ~0.6 MB/s
            // MEDIUM (720p): ~1.2 MB/s
            // HIGH (1080p+): ~2.0 MB/s
            long fileSizeBytes = videoFile.length();
            long fileSizeMB = fileSizeBytes / (1024 * 1024);
            
            double bitrateMBps = video.getQuality() != null ? 
                video.getQuality().getBitrateMBps() : 1.2; // Default MEDIUM
            
            long estimatedSeconds = Math.max(10, (long)(fileSizeBytes / (bitrateMBps * 1024 * 1024)));
            
            System.out.println("[DURATION] Veličina: " + fileSizeMB + "MB");
            System.out.println("[DURATION] Kvalitet: " + video.getQuality() + " (" + bitrateMBps + " MB/s)");
            System.out.println("[DURATION] Procena (heuristika): " + estimatedSeconds + "s");
            
            // Ograniči na maksimalno 2 sata (razumno za streaming)
            long result = Math.min(estimatedSeconds, 7200);
            System.out.println("[DURATION] Finalno: " + result + "s");
            
            return result;
        } catch (Exception e) {
            return DEFAULT_DURATION_SECONDS;
        }
    }

    /**
     * Pokušava pročitati trajanje iz MP4 metapodataka koristeći isoparser
     * Vraća trajanje u sekundama ili null ako nije moguće
     */
    private Long getMp4DurationSeconds(File videoFile) {
        try (IsoFile iso = new IsoFile(videoFile.getAbsolutePath())) {
            java.util.List<MovieBox> movieBoxes = iso.getBoxes(MovieBox.class);
            if (movieBoxes == null || movieBoxes.isEmpty()) return null;

            MovieBox mv = movieBoxes.get(0);
            MovieHeaderBox mvhd = mv.getMovieHeaderBox();
            if (mvhd == null) return null;

            long timescale = mvhd.getTimescale();
            long duration = mvhd.getDuration();
            if (timescale <= 0) return null;

            long seconds = (duration + timescale - 1) / timescale; // ceil
            return seconds;
        } catch (Exception e) {
            System.out.println("[DURATION] Metadata read failed: " + e.getMessage());
            return null;
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

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));
        System.out.println("[SCHEDULE] Zahtev za video #" + videoId);
        System.out.println("[SCHEDULE] Sada: " + now);
        System.out.println("[SCHEDULE] Zakazano: " + scheduledAt);
        
        if (scheduledAt != null && scheduledAt.isBefore(now)) {
            System.out.println("[SCHEDULE] Greškaže je u prošlosti!");
            throw new IllegalArgumentException("Zakazano vreme ne može biti u prošlosti");
        }

        video.setScheduledAt(scheduledAt);
        videoRepository.save(video);
        System.out.println("[SCHEDULE] Uspešno zakazano!");
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
