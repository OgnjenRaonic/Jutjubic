package com.example.demo.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private static class Entry { int count; long resetAt; }

    private final Map<String, Entry> attempts = new ConcurrentHashMap<>();
    private final int MAX = 5;
    private final long WINDOW_MS = 60_000L; // 1 minute

    public void loginSucceeded(String ip) {
        attempts.remove(ip);
    }

    public void loginFailed(String ip) {
        Entry e = attempts.computeIfAbsent(ip, k -> { Entry n = new Entry(); n.count=0; n.resetAt=Instant.now().toEpochMilli()+WINDOW_MS; return n; });
        synchronized (e) {
            long now = Instant.now().toEpochMilli();
            if (now > e.resetAt) { e.count = 0; e.resetAt = now + WINDOW_MS; }
            e.count++;
        }
    }

    public boolean isBlocked(String ip) {
        Entry e = attempts.get(ip);
        if (e == null) return false;
        long now = Instant.now().toEpochMilli();
        if (now > e.resetAt) {
            attempts.remove(ip);
            return false;
        }
        return e.count >= MAX;
    }
}
