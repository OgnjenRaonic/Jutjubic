package com.example.demo.security;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommentRateLimiter {
    private static final int MAX_COMMENTS_PER_HOUR = 60;
    private static final long HOUR_NANOS = 60 * 60 * 1_000_000_000L; // 1 hour in nanoseconds

    // Čuva timestamp zadnjih komentara po user ID-u
    private final Map<Long, java.util.LinkedList<Long>> userCommentTimestamps = new ConcurrentHashMap<>();

    /**
     * Proverava da li korisnik može da pošalje novi komentar
     */
    public boolean canComment(Long userId) {
        var timestamps = userCommentTimestamps.getOrDefault(userId, new java.util.LinkedList<>());
        long now = System.nanoTime();
        long oneHourAgo = now - HOUR_NANOS;

        // Očisti stare timestamps
        timestamps.removeIf(ts -> ts < oneHourAgo);

        return timestamps.size() < MAX_COMMENTS_PER_HOUR;
    }

    /**
     * Beleži novi komentar
     */
    public void recordComment(Long userId) {
        var timestamps = userCommentTimestamps.computeIfAbsent(userId, k -> new java.util.LinkedList<>());
        timestamps.add(System.nanoTime());

        // Očisti jako stare redove iz mape
        long now = System.nanoTime();
        long oneHourAgo = now - HOUR_NANOS;
        if (timestamps.isEmpty() || timestamps.getLast() < oneHourAgo) {
            userCommentTimestamps.remove(userId);
        }
    }

    /**
     * Dobija broj komentara koje je korisnik poslao u poslednjem satu
     */
    public int getCommentsInLastHour(Long userId) {
        var timestamps = userCommentTimestamps.getOrDefault(userId, new java.util.LinkedList<>());
        long now = System.nanoTime();
        long oneHourAgo = now - HOUR_NANOS;
        return (int) timestamps.stream().filter(ts -> ts > oneHourAgo).count();
    }

    /**
     * Dobija vreme kada će korisnik moći da pošalje sledeći komentar
     */
    public LocalDateTime getNextAvailableTime(Long userId) {
        var timestamps = userCommentTimestamps.getOrDefault(userId, new java.util.LinkedList<>());
        if (timestamps.isEmpty() || timestamps.size() < MAX_COMMENTS_PER_HOUR) {
            return null;
        }
        
        long oldestTimestamp = timestamps.getFirst();
        long nextAvailableNanos = oldestTimestamp + HOUR_NANOS;
        return LocalDateTime.now().plus(java.time.Duration.ofNanos(nextAvailableNanos - System.nanoTime()));
    }
}
