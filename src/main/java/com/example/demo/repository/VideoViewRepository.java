package com.example.demo.repository;

import com.example.demo.model.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    @Query("""
        SELECT vv.video.id, COUNT(vv.id)
        FROM VideoView vv
        WHERE vv.viewedAt >= :since
          AND vv.cellLat BETWEEN :minCellLat AND :maxCellLat
          AND vv.cellLon BETWEEN :minCellLon AND :maxCellLon
        GROUP BY vv.video.id
        ORDER BY COUNT(vv.id) DESC
    """)
    List<Object[]> topVideoIdsInCellsSince(
            @Param("since") Instant since,
            @Param("minCellLat") int minCellLat,
            @Param("maxCellLat") int maxCellLat,
            @Param("minCellLon") int minCellLon,
            @Param("maxCellLon") int maxCellLon
    );

    @Query("""
        SELECT vv.video.id, COUNT(vv.id)
        FROM VideoView vv
        WHERE vv.viewedAt >= :since
          AND vv.geohash IN :hashes
        GROUP BY vv.video.id
        ORDER BY COUNT(vv.id) DESC
    """)
    List<Object[]> topVideoIdsInHashesSince(
            @Param("since") Instant since,
            @Param("hashes") List<String> hashes
    );
}
