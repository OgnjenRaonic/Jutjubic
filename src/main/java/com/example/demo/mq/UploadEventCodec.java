package com.example.demo.mq;

public interface UploadEventCodec {
    String name();

    byte[] serialize(UploadEvent event);

    UploadEvent deserialize(byte[] payload);
}