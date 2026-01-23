package com.example.demo.controller;

import com.example.demo.dtos.CommentDTO;
import com.example.demo.dtos.CommentPageResponse;
import com.example.demo.dtos.CreateCommentDTO;
import com.example.demo.dtos.RateLimitInfoDTO;
import com.example.demo.service.CommentService;
import com.example.demo.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    /**
     * GET /api/comments/video/{videoId}?page=0&pageSize=10
     * Dobija komentare za video sa paginacijom
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<?> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        if (pageSize > 100) pageSize = 100;
        if (page < 0) page = 0;

        try {
            CommentPageResponse response = commentService.getComments(videoId, page, pageSize);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Greška pri učitavanju komentara: " + e.getMessage());
        }
    }

    /**
     * POST /api/comments/video/{videoId}
     * Kreira novi komentar (samo autentificirani korisnici)
     */
    @PostMapping("/video/{videoId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long videoId,
            @RequestBody CreateCommentDTO dto,
            Authentication auth) {

        if (auth == null) {
            return ResponseEntity.status(401).body("Morate biti prijavljeni da komentirate");
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long userId = userDetails.getUser().getId();

            // Proveri rate limit
            RateLimitInfoDTO rateLimitInfo = commentService.getRateLimitInfo(userId);
            if (!rateLimitInfo.isCanComment()) {
                return ResponseEntity.status(429).body(
                    "Prekoračili ste limit od 60 komentara po satu. Pokušajte kasnije."
                );
            }

            CommentDTO comment = commentService.createComment(videoId, userId, dto);
            return ResponseEntity.status(201).body(comment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Greška pri kreiranju komentara: " + e.getMessage());
        }
    }

    /**
     * GET /api/comments/rate-limit-info
     * Dobija rate limit informacije za trenutnog korisnika
     */
    @GetMapping("/rate-limit-info")
    public ResponseEntity<?> getRateLimitInfo(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body("Morate biti prijavljeni");
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long userId = userDetails.getUser().getId();

            RateLimitInfoDTO rateLimitInfo = commentService.getRateLimitInfo(userId);
            return ResponseEntity.ok(rateLimitInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Greška pri čitanju rate limit info: " + e.getMessage());
        }
    }
}
