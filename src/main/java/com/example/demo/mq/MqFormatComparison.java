package com.example.demo.mq;

import java.util.List;

public final class MqFormatComparison {
    private static final int DEFAULT_MESSAGE_COUNT = 100;
    private static final long DEFAULT_SEED = 42L;

    private MqFormatComparison() {
    }

    public static void main(String[] args) {
        int messageCount = parseMessageCount(args);
        List<UploadEvent> events = UploadEventGenerator.generate(messageCount, DEFAULT_SEED);

        UploadEventCodec jsonCodec = new UploadEventJsonCodec();
        UploadEventCodec protobufCodec = new UploadEventProtobufCodec();

        warmUp(events, jsonCodec, protobufCodec);

        ComparisonResult jsonResult = benchmark(jsonCodec, events);
        ComparisonResult protobufResult = benchmark(protobufCodec, events);

        printHeader(messageCount);
        printResult(jsonResult);
        printResult(protobufResult);
        printComparison(jsonResult, protobufResult);
    }

    private static int parseMessageCount(String[] args) {
        int count = DEFAULT_MESSAGE_COUNT;
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid message count provided, using default: " + DEFAULT_MESSAGE_COUNT);
                count = DEFAULT_MESSAGE_COUNT;
            }
        }
        if (count < 50) {
            count = 50;
        }
        return count;
    }

    private static void warmUp(List<UploadEvent> events, UploadEventCodec... codecs) {
        int warmCount = Math.min(10, events.size());
        List<UploadEvent> subset = events.subList(0, warmCount);
        for (UploadEventCodec codec : codecs) {
            for (UploadEvent event : subset) {
                byte[] payload = codec.serialize(event);
                UploadEvent decoded = codec.deserialize(payload);
                if (decoded == null) {
                    throw new IllegalStateException("Warmup failed for " + codec.name());
                }
            }
        }
    }

    private static ComparisonResult benchmark(UploadEventCodec codec, List<UploadEvent> events) {
        long serializeNanos = 0L;
        long deserializeNanos = 0L;
        long totalSizeBytes = 0L;
        long checksum = 0L;

        byte[][] payloads = new byte[events.size()][];

        for (int i = 0; i < events.size(); i++) {
            UploadEvent event = events.get(i);
            long start = System.nanoTime();
            byte[] payload = codec.serialize(event);
            long end = System.nanoTime();
            serializeNanos += (end - start);
            totalSizeBytes += payload.length;
            payloads[i] = payload;
        }

        for (byte[] payload : payloads) {
            long start = System.nanoTime();
            UploadEvent event = codec.deserialize(payload);
            long end = System.nanoTime();
            deserializeNanos += (end - start);
            checksum += event.hashCode();
        }

        double avgSerializeMicros = serializeNanos / 1_000.0 / events.size();
        double avgDeserializeMicros = deserializeNanos / 1_000.0 / events.size();
        double avgSizeBytes = (double) totalSizeBytes / events.size();

        return new ComparisonResult(codec.name(), avgSerializeMicros, avgDeserializeMicros, avgSizeBytes, checksum);
    }

    private static void printHeader(int messageCount) {
        System.out.println("MQ JSON vs Protobuf comparison");
        System.out.println("Messages: " + messageCount);
        System.out.println("Fields: id, title, sizeBytes, authorId, authorName, uploadTimestampEpochMs, durationSeconds, tags, visibility");
        System.out.println();
    }

    private static void printResult(ComparisonResult result) {
        System.out.println(result.label());
        System.out.printf("  Avg serialize: %.2f microseconds%n", result.avgSerializeMicros());
        System.out.printf("  Avg deserialize: %.2f microseconds%n", result.avgDeserializeMicros());
        System.out.printf("  Avg size: %.2f bytes%n", result.avgSizeBytes());
        System.out.printf("  Checksum: %d%n", result.checksum());
        System.out.println();
    }

    private static void printComparison(ComparisonResult json, ComparisonResult protobuf) {
        double sizeRatio = json.avgSizeBytes() / protobuf.avgSizeBytes();
        double serializeRatio = json.avgSerializeMicros() / protobuf.avgSerializeMicros();
        double deserializeRatio = json.avgDeserializeMicros() / protobuf.avgDeserializeMicros();

        System.out.printf("Size ratio (JSON / Protobuf): %.2f%n", sizeRatio);
        System.out.printf("Serialize time ratio (JSON / Protobuf): %.2f%n", serializeRatio);
        System.out.printf("Deserialize time ratio (JSON / Protobuf): %.2f%n", deserializeRatio);
    }

    private record ComparisonResult(
            String label,
            double avgSerializeMicros,
            double avgDeserializeMicros,
            double avgSizeBytes,
            long checksum
    ) {
    }
}