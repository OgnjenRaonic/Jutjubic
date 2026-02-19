package com.example.demo.mq;

import java.util.List;

public record UploadEvent(
        String id,
        String title,
        long sizeBytes,
        String authorId,
        String authorName,
        long uploadTimestampEpochMs,
        int durationSeconds,
        List<String> tags,
        String visibility
) {
    public UploadEvent {
        if (tags == null) {
            tags = List.of();
        } else {
            tags = List.copyOf(tags);
        }
    }
}