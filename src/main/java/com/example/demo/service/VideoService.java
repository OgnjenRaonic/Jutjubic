package com.example.demo.service;

import com.example.demo.dtos.CreateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoService {

    VideoDTO create(CreateVideoDTO data, MultipartFile thumbnail, MultipartFile video, String ownerEmail);

    List<VideoDTO> listNewestFirst();
    VideoDTO getById(Long id);

    Resource getVideoResource(Long id) throws IOException;

    ThumbnailPayload loadThumbnail(Long id) throws IOException;

    record ThumbnailPayload(byte[] bytes, String contentType) {}
}
