package com.example.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    // Umesto aktivnih sesija, pratimo unique korisnike po danu
    // Map: username -> poslednji login timestamp
    private final Map<String, Instant> uniqueUsers24h = new ConcurrentHashMap<>();

    // Set za brzi broj unique korisnika
    private final AtomicInteger uniqueUsersCount = new AtomicInteger(0);

    private Counter videoViewsCounter;
    private Counter apiRequestsCounter;
    private Timer videoStreamTimer;

    @PostConstruct
    public void init() {
        // Gauge za unique korisnike u 24h
        Gauge.builder("jutjubic.unique.users.24h", uniqueUsersCount, AtomicInteger::get)
                .description("Broj jedinstvenih korisnika u poslednjih 24 sata")
                .register(meterRegistry);

        videoViewsCounter = Counter.builder("jutjubic.video.views")
                .description("Ukupan broj pregleda videa")
                .register(meterRegistry);

        apiRequestsCounter = Counter.builder("jutjubic.api.requests")
                .description("Ukupan broj API zahteva")
                .register(meterRegistry);

        videoStreamTimer = Timer.builder("jutjubic.video.stream.duration")
                .description("Vreme trajanja stream-a videa")
                .register(meterRegistry);

        // Pokreni cleanup task svakih 5 minuta
        startCleanupTask();
    }

    /**
     * Registruje korisnika koji se ulogovao
     */
    public void userLoggedIn(String username) {
        if (username == null || username.isEmpty()) return;

        uniqueUsers24h.put(username, Instant.now());
        uniqueUsersCount.set(uniqueUsers24h.size());


    }

    /**
     * Čisti stare zapise i ažurira brojač
     */
    private void updateUniqueUsersCount() {
        // Ukloni korisnike starije od 24h
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        uniqueUsers24h.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        // Ažuriraj brojač
        uniqueUsersCount.set(uniqueUsers24h.size());
    }

    /**
     * Pokreće periodično čišćenje
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(5 * 60 * 1000); // Svakih 5 minuta
                    updateUniqueUsersCount();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * Vraća broj unique korisnika u poslednjih 24h
     */
    public int getUniqueUsersCount() {
        updateUniqueUsersCount();
        return uniqueUsersCount.get();
    }

    // ... ostale metode ostaju iste ...

    public void incrementVideoView() {
        videoViewsCounter.increment();
    }

    public void incrementApiRequest() {
        apiRequestsCounter.increment();
    }

    public void recordStreamDuration(long durationMillis) {
        videoStreamTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}