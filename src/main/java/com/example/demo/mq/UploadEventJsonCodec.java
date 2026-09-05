package com.example.demo.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class UploadEventJsonCodec implements UploadEventCodec {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "JSON";
    }

    @Override
    public byte[] serialize(UploadEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    @Override
    public UploadEvent deserialize(byte[] payload) {
        try {
            return objectMapper.readValue(payload, UploadEvent.class);
        } catch (IOException e) {
            throw new IllegalStateException("JSON deserialization failed", e);
        }
    }
}