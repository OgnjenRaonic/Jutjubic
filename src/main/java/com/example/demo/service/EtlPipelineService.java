package com.example.demo.service;

import com.example.demo.dtos.PopularVideoDTO;
import com.example.demo.model.PopularVideoDaily;
import com.example.demo.model.Video;
import com.example.demo.model.VideoView;
import com.example.demo.repository.PopularVideoDailyRepository;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.VideoViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

@Service
public class EtlPipelineService {

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private PopularVideoDailyRepository popularVideoDailyRepository;

    /**
     * Pokreće se svakog dana u 3:00 ujutru
     * cron = "sekunda minut sat dan mesec danUNedelji"
     */
    @PostConstruct
    public void runOnStartup() {
        System.out.println("[ETL] Pokretanje pri startup-u...");
        runDailyEtlPipeline();
    }
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void runDailyEtlPipeline() {
        System.out.println("[ETL] Pokretanje pipeline-a: " + Instant.now());

        LocalDate today = LocalDate.now();

        // EXTRACT: Dobavi sve preglede iz poslednjih 7 dana
        List<VideoView> last7DaysViews = extractLast7DaysViews();

        // TRANSFORM: Izračunaj popularity score
        List<VideoScore> scoredVideos = transformCalculateScores(last7DaysViews);

        // LOAD: Sačuvaj top 3 u bazu
        loadTop3ToDatabase(scoredVideos, today);

        System.out.println("[ETL] Pipeline završen. Top 3 video sačuvana.");
    }

    /**
     * EXTRACT: Iščitaj preglede iz poslednjih 7 dana
     */
    private List<VideoView> extractLast7DaysViews() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        List<VideoView> allViews = videoViewRepository.findAll();
        System.out.println("[ETL DEBUG] Ukupno pregleda u bazi: " + allViews.size());

        List<VideoView> filtered = allViews.stream()
                .filter(vv -> {
                    boolean isAfter = vv.getViewedAt().isAfter(sevenDaysAgo);
                    System.out.println("[ETL DEBUG] Pregled ID=" + vv.getId() +
                            ", video=" + vv.getVideo().getId() +
                            ", vreme=" + vv.getViewedAt() +
                            ", isAfter7Days=" + isAfter);
                    return isAfter;
                })
                .collect(Collectors.toList());

        System.out.println("[ETL DEBUG] Pregleda u poslednjih 7 dana: " + filtered.size());

        return filtered;
    }

    /**
     * TRANSFORM: Grupiši po videu i izračunaj weighted score
     *
     * Težine:
     * - Dan 1 (prethodni dan): težina 7
     * - Dan 2: težina 6
     * - Dan 3: težina 5
     * - Dan 4: težina 4
     * - Dan 5: težina 3
     * - Dan 6: težina 2
     * - Dan 7: težina 1
     */
    private List<VideoScore> transformCalculateScores(List<VideoView> views) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZoneId zone = ZoneId.systemDefault();

        // Grupiši preglede po video ID
        Map<Long, List<VideoView>> viewsByVideo = views.stream()
                .collect(Collectors.groupingBy(vv -> vv.getVideo().getId()));

        List<VideoScore> scores = new ArrayList<>();

        for (Map.Entry<Long, List<VideoView>> entry : viewsByVideo.entrySet()) {
            Long videoId = entry.getKey();
            List<VideoView> videoViews = entry.getValue();

            // Izračunaj preglede po danima
            long[] dailyViews = new long[8]; // index 1-7 za dane

            for (VideoView vv : videoViews) {
                LocalDate viewDate = vv.getViewedAt()
                        .atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(zone)
                        .toLocalDate();
                int daysAgo = (int) ChronoUnit.DAYS.between(viewDate, today);
                System.out.println("[ETL DEBUG] Video=" + videoId +
                        ", viewDate=" + viewDate +
                        ", today=" + today +
                        ", daysAgo=" + daysAgo);
                if (daysAgo >= 0 && daysAgo <= 6) {
                    dailyViews[daysAgo + 1]++;
                }
            }

            // Izračunaj weighted score
            // Dan 1 (juče) * 7 + Dan 2 * 6 + ... + Dan 7 * 1
            double score = 0;
            score += dailyViews[1] * 7; // prethodni dan
            score += dailyViews[2] * 6;
            score += dailyViews[3] * 5;
            score += dailyViews[4] * 4;
            score += dailyViews[5] * 3;
            score += dailyViews[6] * 2;
            score += dailyViews[7] * 1; // pre 7 dana

            long totalViews = dailyViews[1] + dailyViews[2] + dailyViews[3] + dailyViews[4] +
                    dailyViews[5] + dailyViews[6] + dailyViews[7];

            scores.add(new VideoScore(videoId, score, totalViews, dailyViews));
        }

        // Sortiraj po score-u opadajuće
        scores.sort(Comparator.comparingDouble(VideoScore::getScore).reversed());

        return scores;
    }

    /**
     * LOAD: Sačuvaj top 3 u bazu
     */
    private void loadTop3ToDatabase(List<VideoScore> scoredVideos, LocalDate calculationDate) {
        System.out.println("[ETL DEBUG] Ukupno video sa score-om: " + scoredVideos.size());

        // Obriši stare zapise
        List<PopularVideoDaily> existing = popularVideoDailyRepository
                .findByCalculationDateOrderByRankPositionAsc(calculationDate);
        popularVideoDailyRepository.deleteAll(existing);
        System.out.println("[ETL DEBUG] Obrisano starih zapisa: " + existing.size());

        // Sačuvaj top 3
        int rank = 1;
        int saved = 0;
        for (VideoScore vs : scoredVideos.subList(0, Math.min(3, scoredVideos.size()))) {
            System.out.println("[ETL DEBUG] Čuvam rank " + rank + ": video=" + vs.getVideoId() +
                    ", score=" + vs.getScore() + ", totalViews=" + vs.getTotalViews());

            Video video = videoRepository.findById(vs.getVideoId()).orElse(null);
            if (video == null) {
                System.out.println("[ETL DEBUG] Video nije pronađen: " + vs.getVideoId());
                continue;
            }

            PopularVideoDaily pvd = new PopularVideoDaily();
            pvd.setCalculationDate(calculationDate);
            pvd.setCalculatedAt(Instant.now());
            pvd.setRankPosition(rank++);
            pvd.setVideo(video);
            pvd.setPopularityScore(vs.getScore());
            pvd.setViewsDay1(vs.getDailyViews()[1]);
            pvd.setViewsDay2(vs.getDailyViews()[2]);
            pvd.setViewsDay3(vs.getDailyViews()[3]);
            pvd.setViewsDay4(vs.getDailyViews()[4]);
            pvd.setViewsDay5(vs.getDailyViews()[5]);
            pvd.setViewsDay6(vs.getDailyViews()[6]);
            pvd.setViewsDay7(vs.getDailyViews()[7]);

            popularVideoDailyRepository.save(pvd);
            saved++;
            System.out.println("[ETL DEBUG] Sačuvano: video=" + video.getId() + " sa ukupno " +
                    (vs.getDailyViews()[1] + vs.getDailyViews()[2] + vs.getDailyViews()[3] +
                            vs.getDailyViews()[4] + vs.getDailyViews()[5] + vs.getDailyViews()[6] + vs.getDailyViews()[7]) +
                    " pregleda");
        }

        System.out.println("[ETL DEBUG] Ukupno sačuvano: " + saved);
    }

    /**
     * Vraća top 3 popularna videa za prikaz na frontend-u
     */
    public List<PopularVideoDTO> getTop3PopularVideos() {
        LocalDate today = LocalDate.now();

        // Probaj da nađeš za danas
        List<PopularVideoDaily> top3 = popularVideoDailyRepository
                .findByCalculationDateOrderByRankPositionAsc(today);

        // Ako nema za danas, uzmi najnoviji
        if (top3.isEmpty()) {
            top3 = popularVideoDailyRepository
                    .findTopByOrderByCalculationDateDescRankPositionAsc()
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }

        return top3.stream()
                .map(pvd -> new PopularVideoDTO(
                        pvd.getVideo().getId(),
                        pvd.getVideo().getTitle(),
                        "/api/videos/" + pvd.getVideo().getId() + "/thumbnail",
                        pvd.getRankPosition(),
                        pvd.getPopularityScore(),
                        pvd.getViewsDay1() + pvd.getViewsDay2() + pvd.getViewsDay3() +
                                pvd.getViewsDay4() + pvd.getViewsDay5() + pvd.getViewsDay6() + pvd.getViewsDay7()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Pomoćna klasa za čuvanje score-a
     */
    private static class VideoScore {
        private final Long videoId;
        private final double score;
        private final long totalViews;
        private final long[] dailyViews;

        public VideoScore(Long videoId, double score, long totalViews, long[] dailyViews) {
            this.videoId = videoId;
            this.score = score;
            this.totalViews = totalViews;
            this.dailyViews = dailyViews;
        }

        public Long getVideoId() { return videoId; }
        public double getScore() { return score; }
        public long getTotalViews() { return totalViews; }
        public long[] getDailyViews() { return dailyViews; }
    }
}