package com.example.demo.service;

import com.example.demo.dtos.CreateVideoDTO;
import com.example.demo.dtos.VideoDTO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {
    VideoDTO create(CreateVideoDTO data, MultipartFile thumbnail, MultipartFile videoFile, String username);
}
