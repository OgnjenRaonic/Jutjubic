package com.example.demo.service;

import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.dtos.CommentDTO;
import com.example.demo.dtos.CommentPageResponse;
import com.example.demo.dtos.CreateCommentDTO;
import com.example.demo.dtos.RateLimitInfoDTO;
import com.example.demo.security.CommentRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRateLimiter rateLimiter;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Dobija komentare za video sa paginacijom (sa cache-om)
     */
    @Cacheable(value = "comments", key = "#videoId + '_' + #page + '_' + #pageSize")
    @Transactional(readOnly = true)
    public CommentPageResponse getComments(Long videoId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Comment> commentPage = commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);

        var content = commentPage.getContent().stream()
            .map(this::convertToDTO)
            .toList();

        return new CommentPageResponse(
            content,
            page,
            pageSize,
            commentPage.getTotalElements(),
            commentPage.getTotalPages()
        );
    }

    /**
     * Kreira novi komentar
     */
    @CacheEvict(value = "comments", allEntries = true)
    @Transactional
    public CommentDTO createComment(Long videoId, Long userId, CreateCommentDTO dto) {
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Komentar ne može biti prazan");
        }

        if (dto.getText().length() > 1000) {
            throw new IllegalArgumentException("Komentar ne može biti duži od 1000 karaktera");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));

        Comment comment = new Comment(videoId, user, dto.getText());
        Comment saved = commentRepository.save(comment);

        // Beleži komentar za rate limiting
        rateLimiter.recordComment(userId);

        return convertToDTO(saved);
    }

    /**
     * Dobija rate limit informacije za korisnika
     */
    @Transactional(readOnly = true)
    public RateLimitInfoDTO getRateLimitInfo(Long userId) {
        boolean canComment = rateLimiter.canComment(userId);
        int commentsInLastHour = rateLimiter.getCommentsInLastHour(userId);
        
        LocalDateTime nextAvailable = null;
        if (!canComment) {
            nextAvailable = rateLimiter.getNextAvailableTime(userId);
        }

        String nextAvailableStr = nextAvailable != null ? nextAvailable.format(ISO_FORMATTER) : null;

        return new RateLimitInfoDTO(commentsInLastHour, canComment, nextAvailableStr);
    }

    /**
     * Konvertuje Comment u CommentDTO
     */
    private CommentDTO convertToDTO(Comment comment) {
        return new CommentDTO(
            comment.getId(),
            comment.getVideoId(),
            comment.getUser().getId(),
            comment.getUser().getUsername(),
            comment.getText(),
            comment.getCreatedAt().format(ISO_FORMATTER)
        );
    }
}
