package com.example.demo.mq;

import com.example.demo.proto.UploadEventMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public final class UploadEventProtobufCodec implements UploadEventCodec {
    @Override
    public String name() {
        return "Protobuf";
    }

    @Override
    public byte[] serialize(UploadEvent event) {
        UploadEventMessage message = UploadEventMessage.newBuilder()
                .setId(nullToEmpty(event.id()))
                .setTitle(nullToEmpty(event.title()))
                .setSizeBytes(event.sizeBytes())
                .setAuthorId(nullToEmpty(event.authorId()))
                .setAuthorName(nullToEmpty(event.authorName()))
                .setUploadTimestampEpochMs(event.uploadTimestampEpochMs())
                .setDurationSeconds(event.durationSeconds())
                .addAllTags(event.tags())
                .setVisibility(nullToEmpty(event.visibility()))
                .build();
        return message.toByteArray();
    }

    @Override
    public UploadEvent deserialize(byte[] payload) {
        try {
            UploadEventMessage message = UploadEventMessage.parseFrom(payload);
            return new UploadEvent(
                    message.getId(),
                    message.getTitle(),
                    message.getSizeBytes(),
                    message.getAuthorId(),
                    message.getAuthorName(),
                    message.getUploadTimestampEpochMs(),
                    message.getDurationSeconds(),
                    message.getTagsList(),
                    message.getVisibility()
            );
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Protobuf deserialization failed", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}