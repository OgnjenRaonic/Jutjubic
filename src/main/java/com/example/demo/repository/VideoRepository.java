package com.example.demo.repository;

import com.example.demo.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findAllByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query("select v.viewCount from Video v where v.id = :id")
    Long getViewCount(@Param("id") Long id);
}
