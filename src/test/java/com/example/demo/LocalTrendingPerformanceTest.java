package com.example.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.VideoDTO;
import com.example.demo.model.GeoPoint;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.VideoViewRepository;
import com.example.demo.service.TrendingService;

@SpringBootTest
@ActiveProfiles("test")
public class LocalTrendingPerformanceTest {

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;

    private List<Long> testVideoIds = new ArrayList<>();
    private com.example.demo.model.User testAuthor;

    @BeforeEach
    @Transactional
    public void setup() {
        // Proverite da li postoje već podaci, ako da skočite seed
        long videoCount = videoRepository.count();
        if (videoCount > 50) {
            System.out.println("\n=== TEST SETUP (Existing Data) ===");
            System.out.println("Videos in DB: " + videoCount);
            System.out.println("Video Views: " + videoViewRepository.count());
            testVideoIds = videoRepository.findAll().stream()
                    .map(com.example.demo.model.Video::getId)
                    .limit(50)
                    .toList();
            return;
        }

        // Kreiraj test korisnika (autora videa)
        testAuthor = new com.example.demo.model.User();
        testAuthor.setEmail("trending_test_author@test.com");
        testAuthor.setUsername("trending_test_author");
        testAuthor.setPassword("$2a$10$slYQmyNdGzin7olVLc1DO.QmCiUBu6f0Hc0gC4g0G2c6v3yYQZMGu");
        testAuthor.setEnabled(true);
        testAuthor = userRepository.save(testAuthor);

        // Kreiraj 100 videa sa raznovrsnim svojstvima
        List<String> tags = Arrays.asList("action", "comedy", "drama", "music", "sports",
                "tutorial", "vlog", "gaming", "cooking", "travel");

        for (int i = 0; i < 100; i++) {
            com.example.demo.model.Video video = new com.example.demo.model.Video();
            video.setAuthor(testAuthor);
            video.setTitle("Trending Video " + i);
            video.setDescription("Test video " + i + " for trending performance");
            video.setViewCount((long) (Math.random() * 500));
            video.setCommentCount((long) (Math.random() * 100));
            video.setThumbnailPath("/thumbnails/video_" + i + ".jpg");
            video.setVideoPath("/videos/video_" + i + ".mp4");
            video.setTags(Arrays.asList(tags.get(i % tags.size())));
            video.setCreatedAt(Instant.now().minusSeconds((long) (Math.random() * 604800))); // Within last 7 days
            videoRepository.save(video);
            testVideoIds.add(video.getId());
        }

        // Kreiraj 2000 video_view zapisa raspoređenih po gradovima
        List<double[]> locations = Arrays.asList(
                new double[]{45.2671, 19.8335}, // Beograd
                new double[]{45.3150, 19.8015}, // Autokomanda
                new double[]{45.2517, 19.8369}, // Novi Sad
                new double[]{43.3209, 21.8954}  // Niš
        );

        for (int i = 0; i < 2000; i++) {
            double[] loc = locations.get(i % locations.size());
            double lat = loc[0] + (Math.random() - 0.5) * 0.05;
            double lon = loc[1] + (Math.random() - 0.5) * 0.05;

            com.example.demo.model.Video video = videoRepository.findById(
                    testVideoIds.get((int) (Math.random() * testVideoIds.size()))
            ).orElse(null);
            if (video == null) continue;

            int cellLat = (int) Math.floor(lat / 0.01);
            int cellLon = (int) Math.floor(lon / 0.01);
            String geohash = com.example.demo.util.GeohashUtil.encode(lat, lon, 5);

            com.example.demo.model.VideoView view = new com.example.demo.model.VideoView(video, lat, lon, "GPS", cellLat, cellLon, geohash);
            videoViewRepository.save(view);
        }

        System.out.println("\n=== TEST SETUP (Seeded) ===");
        System.out.println("Videos created: " + videoRepository.count());
        System.out.println("Video Views created: " + videoViewRepository.count());
    }

    /**
     * Test 1: Osnovna latency merenja za različite radijuse
     * Meri vreme potrebno da se dobiju trending videa za lokaciju sa različitim radiusima
     */
    @Test
    public void testTrendingLatencyByRadius() {
        System.out.println("\n=== TEST 1: Latency by Radius ===");

        // Test lokacije (glavne gradske oblasti)
        List<GeoPoint> testLocations = Arrays.asList(
                new GeoPoint(45.2671, 19.8335, "Beograd Centar"),  // Beograd
                new GeoPoint(45.3150, 19.8015, "Beograd Autokomanda"),
                new GeoPoint(45.2517, 19.8369, "Novi Sad"),
                new GeoPoint(43.3209, 21.8954, "Niš")
        );

        // Radijusi za testiranje (km)
        double[] radii = {5, 10, 20, 50};

        Map<String, LatencyStats> results = new LinkedHashMap<>();

        for (double radius : radii) {
            String key = radius + "km";
            LatencyStats stats = new LatencyStats(key);

            // Za svaku lokaciju, meri vreme
            for (GeoPoint location : testLocations) {
                for (int attempt = 0; attempt < 5; attempt++) {  // 5 pokušaja po lokaciji
                    long startTime = System.nanoTime();
                    try {
                        List<VideoDTO> trending = trendingService.findLocalTrending(location, radius);
                        long endTime = System.nanoTime();
                        long latencyMs = (endTime - startTime) / 1_000_000;

                        stats.addMeasurement(latencyMs);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            }

            results.put(key, stats);
        }

        // Ispis rezultata
        printLatencyTable(results);
    }

    /**
     * Test 2: Concurrent zahtevi - simulacija više korisnika istovremeno
     * Meri kako se performanse degradiraju sa povećanjem concurrent zahteva
     */
    @Test
    public void testConcurrentRequests() throws InterruptedException {
        System.out.println("\n=== TEST 2: Concurrent Requests ===");

        GeoPoint testLocation = new GeoPoint(45.2671, 19.8335, "GPS");
        double radius = 10.0;
        int[] concurrencies = {1, 5, 10, 20, 50};

        Map<String, LatencyStats> results = new LinkedHashMap<>();

        for (int concurrency : concurrencies) {
            String key = concurrency + " users";
            LatencyStats stats = new LatencyStats(key);

            // Kreiraj thread pool i pokreni concurrent zahteve
            List<Thread> threads = new ArrayList<>();
            long startGlobalTime = System.nanoTime();

            for (int i = 0; i < concurrency; i++) {
                Thread t = new Thread(() -> {
                    for (int attempt = 0; attempt < 10; attempt++) {  // Svaki thread radi 10 zahteva
                        long startTime = System.nanoTime();
                        try {
                            List<VideoDTO> trending = trendingService.findLocalTrending(testLocation, radius);
                            long endTime = System.nanoTime();
                            long latencyMs = (endTime - startTime) / 1_000_000;
                            stats.addMeasurement(latencyMs);
                        } catch (Exception e) {
                            // Ignoriši greške
                        }
                    }
                });
                threads.add(t);
                t.start();
            }

            // Čekaj da sve niti završe
            for (Thread t : threads) {
                t.join();
            }

            long endGlobalTime = System.nanoTime();
            long totalTimeMs = (endGlobalTime - startGlobalTime) / 1_000_000;
            stats.setTotalExecutionTime(totalTimeMs);

            results.put(key, stats);
        }

        // Ispis rezultata
        printConcurrencyTable(results);
    }

    /**
     * Test 3: Različiti scenariji - kombinuje radijus i konkurentnost
     */
    @Test
    public void testMixedScenarios() throws InterruptedException {
        System.out.println("\n=== TEST 3: Mixed Scenarios ===");

        List<GeoPoint> locations = Arrays.asList(
                new GeoPoint(45.2671, 19.8335, "Beograd"),
                new GeoPoint(45.2517, 19.8369, "Novi Sad"),
                new GeoPoint(43.3209, 21.8954, "Niš")
        );

        Map<String, LatencyStats> results = new LinkedHashMap<>();

        // Scenario 1: Small radius, sequential
        {
            LatencyStats stats = new LatencyStats("Small (5km, 1 user)");
            for (GeoPoint loc : locations) {
                for (int i = 0; i < 10; i++) {
                    long start = System.nanoTime();
                    trendingService.findLocalTrending(loc, 5.0);
                    long end = System.nanoTime();
                    stats.addMeasurement((end - start) / 1_000_000);
                }
            }
            results.put("Small (5km, 1 user)", stats);
        }

        // Scenario 2: Medium radius, sequential
        {
            LatencyStats stats = new LatencyStats("Medium (20km, 1 user)");
            for (GeoPoint loc : locations) {
                for (int i = 0; i < 10; i++) {
                    long start = System.nanoTime();
                    trendingService.findLocalTrending(loc, 20.0);
                    long end = System.nanoTime();
                    stats.addMeasurement((end - start) / 1_000_000);
                }
            }
            results.put("Medium (20km, 1 user)", stats);
        }

        // Scenario 3: Large radius, sequential
        {
            LatencyStats stats = new LatencyStats("Large (50km, 1 user)");
            for (GeoPoint loc : locations) {
                for (int i = 0; i < 10; i++) {
                    long start = System.nanoTime();
                    trendingService.findLocalTrending(loc, 50.0);
                    long end = System.nanoTime();
                    stats.addMeasurement((end - start) / 1_000_000);
                }
            }
            results.put("Large (50km, 1 user)", stats);
        }

        // Scenario 4: Small radius, concurrent (10 korisnika)
        {
            LatencyStats stats = new LatencyStats("Small (5km, 10 users)");
            executeMultiThreadedTest(locations, 5.0, 10, stats);
            results.put("Small (5km, 10 users)", stats);
        }

        // Scenario 5: Large radius, concurrent (10 korisnika)
        {
            LatencyStats stats = new LatencyStats("Large (50km, 10 users)");
            executeMultiThreadedTest(locations, 50.0, 10, stats);
            results.put("Large (50km, 10 users)", stats);
        }

        printLatencyTable(results);
    }

    /**
     * Test 4: Trending videa aktualizacija
     * Meri koliko brzo se novi videi pojavljuju u trending listi
     */
    @Test
    @Transactional
    public void testTrendingFreshness() {
        System.out.println("\n=== TEST 4: Trending Freshness ===");

        GeoPoint location = new GeoPoint(45.2671, 19.8335, "GPS");
        double radius = 10.0;

        // Dobij inicijalni trending
        long startTime = System.nanoTime();
        List<VideoDTO> initialTrending = trendingService.findLocalTrending(location, radius);
        long latencyMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("Initial trending count: " + initialTrending.size());
        System.out.println("Initial query latency: " + latencyMs + "ms");

        // Ponovi zahtev nakon 1 sekunde
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        startTime = System.nanoTime();
        List<VideoDTO> secondTrending = trendingService.findLocalTrending(location, radius);
        latencyMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("Second query latency: " + latencyMs + "ms");
        System.out.println("Result consistency: " + (initialTrending.equals(secondTrending) ? "Stable" : "Changed"));
    }

    // ===================== HELPER METHODS =====================

    private void executeMultiThreadedTest(List<GeoPoint> locations, double radius, int threadCount, LatencyStats stats)
            throws InterruptedException {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread t = new Thread(() -> {
                GeoPoint location = locations.get(threadId % locations.size());
                for (int attempt = 0; attempt < 10; attempt++) {
                    long start = System.nanoTime();
                    try {
                        trendingService.findLocalTrending(location, radius);
                    } catch (Exception e) {
                        // Ignoriši
                    }
                    long end = System.nanoTime();
                    stats.addMeasurement((end - start) / 1_000_000);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    private void printLatencyTable(Map<String, LatencyStats> results) {
        System.out.println("\n┌──────────────────────────┬─────────┬─────────┬─────────┬─────────┐");
        System.out.println("│ Scenario                 │ Avg (ms)│ Min (ms)│ Max (ms)│ P95 (ms)│");
        System.out.println("├──────────────────────────┼─────────┼─────────┼─────────┼─────────┤");

        for (LatencyStats stats : results.values()) {
            System.out.printf("│ %-24s│ %7.2f │ %7d │ %7d │ %7d │%n",
                    stats.name,
                    stats.getAverage(),
                    stats.getMin(),
                    stats.getMax(),
                    stats.getPercentile(95));
        }

        System.out.println("└──────────────────────────┴─────────┴─────────┴─────────┴─────────┘");
    }

    private void printConcurrencyTable(Map<String, LatencyStats> results) {
        System.out.println("\n┌──────────────────────────┬──────────┬─────────┬─────────┬─────────┬──────────────┐");
        System.out.println("│ Scenario                 │ Avg (ms) │ Min (ms)│ Max (ms)│ P95 (ms)│ Total Time(s)│");
        System.out.println("├──────────────────────────┼──────────┼─────────┼─────────┼─────────┼──────────────┤");

        for (LatencyStats stats : results.values()) {
            System.out.printf("│ %-24s│ %8.2f │ %7d │ %7d │ %7d │ %12.2f │%n",
                    stats.name,
                    stats.getAverage(),
                    stats.getMin(),
                    stats.getMax(),
                    stats.getPercentile(95),
                    stats.getTotalExecutionTime() / 1000.0);
        }

        System.out.println("└──────────────────────────┴──────────┴─────────┴─────────┴─────────┴──────────────┘");
    }

    // ===================== HELPER CLASS =====================

    private static class LatencyStats {
        String name;
        List<Long> measurements = new ArrayList<>();
        long totalExecutionTime = 0;

        LatencyStats(String name) {
            this.name = name;
        }

        void addMeasurement(long latencyMs) {
            measurements.add(latencyMs);
        }

        void setTotalExecutionTime(long timeMs) {
            this.totalExecutionTime = timeMs;
        }

        double getAverage() {
            return measurements.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        long getMin() {
            return measurements.stream().mapToLong(Long::longValue).min().orElse(0);
        }

        long getMax() {
            return measurements.stream().mapToLong(Long::longValue).max().orElse(0);
        }

        long getPercentile(int p) {
            List<Long> sorted = measurements.stream()
                    .sorted()
                    .toList();
                if (sorted.isEmpty()) return 0;
                int index = (p * sorted.size()) / 100;
                index = Math.max(0, Math.min(index, sorted.size() - 1));
                return sorted.get(index);
        }

        long getTotalExecutionTime() {
            return totalExecutionTime;
        }
    }
}
