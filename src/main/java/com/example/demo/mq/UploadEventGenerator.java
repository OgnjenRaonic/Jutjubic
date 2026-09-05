package com.example.demo.mq;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class UploadEventGenerator {
    private static final String[] TITLES = {
            "Spring Boot Deep Dive",
            "Docker for Beginners",
            "Kubernetes Crash Course",
            "Async Java Patterns",
            "REST API Design",
            "MQ Fundamentals",
            "Cloud Observability",
            "CI/CD Pipeline",
            "System Design Mock",
            "Database Indexing"
    };

    private static final String[] AUTHORS = {
            "Mila", "Stefan", "Luka", "Jelena", "Ivana",
            "Marko", "Ana", "Nikola", "Tijana", "Petar"
    };

    private static final String[] TAGS = {
            "java", "spring", "backend", "devops", "cloud",
            "microservices", "testing", "security", "postgres", "redis"
    };

    private static final String[] VISIBILITIES = {"PUBLIC", "UNLISTED", "PRIVATE"};

    private UploadEventGenerator() {
    }

    public static List<UploadEvent> generate(int count, long seed) {
        Random random = new Random(seed);
        long now = System.currentTimeMillis();
        List<UploadEvent> events = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String id = deterministicId("video-" + i);
            String title = TITLES[i % TITLES.length] + " #" + (i + 1);
            long sizeBytes = 5_000_000L + random.nextInt(50_000_000);
            String authorName = AUTHORS[random.nextInt(AUTHORS.length)];
            String authorId = deterministicId("author-" + authorName);
            long uploadTimestampEpochMs = now - random.nextInt(3_600_000);
            int durationSeconds = 30 + random.nextInt(900);
            List<String> tags = pickTags(random, 3 + random.nextInt(3));
            String visibility = VISIBILITIES[random.nextInt(VISIBILITIES.length)];

            events.add(new UploadEvent(
                    id,
                    title,
                    sizeBytes,
                    authorId,
                    authorName,
                    uploadTimestampEpochMs,
                    durationSeconds,
                    tags,
                    visibility
            ));
        }

        return events;
    }

    private static String deterministicId(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static List<String> pickTags(Random random, int count) {
        Set<String> selected = new LinkedHashSet<>();
        while (selected.size() < count) {
            selected.add(TAGS[random.nextInt(TAGS.length)]);
        }
        return List.copyOf(selected);
    }
}