package com.example.demo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.ScheduledStreamResponse;
import com.example.demo.model.Video;
import com.example.demo.repository.VideoRepository;

@Service
public class ScheduledStreamingService {

    @Autowired
    private VideoRepository videoRepository;

    @Value("${app.ffmpeg.path:ffprobe}")
    private String ffprobePath;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final long DEFAULT_DURATION_SECONDS = 600;

    @Transactional(readOnly = true)
    public boolean isVideoAvailable(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (video.getScheduledAt() == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));
        return !now.isBefore(video.getScheduledAt());
    }

    @Transactional(readOnly = true)
    public Integer getCurrentStreamOffset(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        if (video.getScheduledAt() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));
        if (now.isBefore(video.getScheduledAt())) {
            return null;
        }

        Duration duration = Duration.between(video.getScheduledAt(), now);
        long offsetSeconds = duration.getSeconds();
        long videoDuration = getVideoDurationSeconds(video);

        if (offsetSeconds >= videoDuration) {
            return (int) videoDuration;
        }

        return (int) offsetSeconds;
    }

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
        } else if (offset != null && offset >= duration) {
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

    private long getVideoDurationSeconds(Video video) {
        System.out.println("[DEBUG] ====================================");
        System.out.println("[DEBUG] Video ID: " + (video != null ? video.getId() : "null"));
        System.out.println("[DEBUG] Video path: " + (video != null ? video.getVideoPath() : "null"));
        System.out.println("[DEBUG] FFprobe path: " + ffprobePath);

        if (video == null || video.getVideoPath() == null || video.getVideoPath().isEmpty()) {
            System.out.println("[DEBUG] Video ili path je null!");
            return DEFAULT_DURATION_SECONDS;
        }

        try {
            File videoFile = new File(video.getVideoPath());
            System.out.println("[DEBUG] Apsolutna putanja: " + videoFile.getAbsolutePath());
            System.out.println("[DEBUG] Fajl postoji? " + videoFile.exists());

            if (!videoFile.exists()) {
                System.out.println("[DEBUG] FAJL NE POSTOJI!");
                return DEFAULT_DURATION_SECONDS;
            }

            System.out.println("[DEBUG] Pokrećem: " + ffprobePath);

            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            System.out.println("[DEBUG] Završio? " + finished);
            System.out.println("[DEBUG] Output: '" + output.toString().trim() + "'");
            System.out.println("[DEBUG] Exit code: " + process.exitValue());

            if (!finished) {
                process.destroyForcibly();
                return DEFAULT_DURATION_SECONDS;
            }

            String result = output.toString().trim();
            if (!result.isEmpty()) {
                try {
                    double durationSeconds = Double.parseDouble(result);
                    if (durationSeconds > 0) {
                        long seconds = Math.round(durationSeconds);
                        System.out.println("[DEBUG] USPEH: " + seconds + " sekundi");
                        return Math.max(10L, Math.min(seconds, 14400L));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[DEBUG] Greška parsiranja: " + e.getMessage());
                }
            }

            System.out.println("[DEBUG] Neuspeh, koristim default");
            return DEFAULT_DURATION_SECONDS;

        } catch (Exception e) {
            System.err.println("[DEBUG] IZUZETAK: " + e.getMessage());
            e.printStackTrace();
            return DEFAULT_DURATION_SECONDS;
        } finally {
            System.out.println("[DEBUG] ====================================");
        }
    }

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

    @Transactional
    public void scheduleVideo(Long videoId, LocalDateTime scheduledAt) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Paris"));

        if (scheduledAt != null && scheduledAt.isBefore(now)) {
            throw new IllegalArgumentException("Zakazano vreme ne može biti u prošlosti");
        }

        video.setScheduledAt(scheduledAt);
        videoRepository.save(video);
    }

    @Transactional
    public void unscheduleVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video nije pronađen"));

        video.setScheduledAt(null);
        videoRepository.save(video);
    }
}