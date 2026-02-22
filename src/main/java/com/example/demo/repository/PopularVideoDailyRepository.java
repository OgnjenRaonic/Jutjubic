package com.example.demo.repository;

import com.example.demo.model.PopularVideoDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PopularVideoDailyRepository extends JpaRepository<PopularVideoDaily, Long> {

    // Pronađi top 3 za određeni datum
    List<PopularVideoDaily> findByCalculationDateOrderByRankPositionAsc(LocalDate calculationDate);

    // Pronađi najnoviji zapis
    Optional<PopularVideoDaily> findTopByOrderByCalculationDateDescRankPositionAsc();

    // Pronađi za video i datum (da ne dupliramo)
    Optional<PopularVideoDaily> findByVideoIdAndCalculationDate(Long videoId, LocalDate calculationDate);
}