package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId, Pageable pageable);
    
    List<Comment> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
}
