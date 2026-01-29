package com.example.demo;

import com.example.demo.model.Video;
import com.example.demo.model.User;
import com.example.demo.repository.VideoRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ScheduledStreamingService;
import com.example.demo.dtos.ScheduledStreamResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ScheduledStreamingTest {

    @Autowired
    private ScheduledStreamingService scheduledStreamingService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private Video testVideo;
    private User testUser;

    @BeforeEach
    @Transactional
    public void setUp() {
        // Kreiraj test korisnika
        testUser = new User();
        testUser.setEmail("streaming@test.com");
        testUser.setUsername("streamingTestUser");
        testUser.setPassword("encoded_password_123");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Kreiraj test video
        testVideo = new Video();
        testVideo.setAuthor(testUser);
        testVideo.setTitle("Test Zakazani Video");
        testVideo.setDescription("Video za testiranje zakazanog streaming-a");
        testVideo.setVideoPath("/tmp/test_video.mp4");
        testVideo.setThumbnailPath("/tmp/test_thumb.jpg");
        testVideo = videoRepository.save(testVideo);
    }

    /**
     * Test: Video zakazan za budućnost nije dostupan
     */
    @Test
    @Transactional
    public void testVideoNotAvailableInFuture() {
        LocalDateTime futureTime = LocalDateTime.now().plus(1, ChronoUnit.HOURS);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), futureTime);

        boolean isAvailable = scheduledStreamingService.isVideoAvailable(testVideo.getId());
        assertFalse(isAvailable, "Video zakazan u budućnosti ne bi trebalo da bude dostupan");

        Integer offset = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());
        assertNull(offset, "Offset trebalo bi da bude null pre početka streaming-a");

        System.out.println("✓ Video nije dostupan pre zakazanog vremena");
    }

    /**
     * Test: Video zakazan u prošlosti je dostupan
     */
    @Test
    @Transactional
    public void testVideoAvailableInPast() {
        LocalDateTime pastTime = LocalDateTime.now().minus(5, ChronoUnit.MINUTES);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), pastTime);

        boolean isAvailable = scheduledStreamingService.isVideoAvailable(testVideo.getId());
        assertTrue(isAvailable, "Video zakazan u prošlosti trebalo bi da bude dostupan");

        Integer offset = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());
        assertNotNull(offset, "Offset trebalo bi da bude dostupan");
        assertTrue(offset >= 300, "Offset trebalo bi da bude >= 300 sekundi (5 minuta)");

        System.out.println("✓ Video je dostupan, offset: " + offset + " sekundi");
    }

    /**
     * Test: Sinhronizovani streaming - offset se računa pravilno
     */
    @Test
    @Transactional
    public void testSynchronizedStreamingOffset() throws InterruptedException {
        LocalDateTime startTime = LocalDateTime.now().minus(2, ChronoUnit.SECONDS);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), startTime);

        Integer offset1 = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());
        assertNotNull(offset1, "Offset trebalo bi da bude dostupan");
        assertTrue(offset1 >= 2, "Prvi offset trebalo bi da bude >= 2 sekunde");

        // Čekaj 1 sekund
        Thread.sleep(1000);

        Integer offset2 = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());
        assertNotNull(offset2, "Offset trebalo bi da bude dostupan nakon čekanja");
        assertTrue(offset2 > offset1, "Drugi offset trebalo bi da bude veći od prvog");

        System.out.println("✓ Sinhronizovani streaming radi: offset1=" + offset1 + "s, offset2=" + offset2 + "s");
    }

    /**
     * Test: Kompletan info o zakazanom videu
     */
    @Test
    @Transactional
    public void testScheduledStreamResponse() {
        LocalDateTime futureTime = LocalDateTime.now().plus(30, ChronoUnit.MINUTES);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), futureTime);

        ScheduledStreamResponse response = scheduledStreamingService.getScheduledStreamInfo(testVideo.getId());

        assertNotNull(response, "Response trebalo bi da bude dostupan");
        assertEquals(testVideo.getId(), response.getVideoId(), "Video ID trebalo bi da se poklapa");
        assertEquals("NOT_STARTED", response.getStreamStatus(), "Status trebalo bi da bude NOT_STARTED");
        assertFalse(response.isAvailable(), "Video ne bi trebalo da bude dostupan");
        assertNull(response.getCurrentOffsetSeconds(), "Offset trebalo bi da bude null");

        System.out.println("✓ Zakazana informacija: " + response.getMessage());
    }

    /**
     * Test: Otkazivanje zakazivanja čini video odmah dostupnim
     */
    @Test
    @Transactional
    public void testUnscheduleVideo() {
        LocalDateTime futureTime = LocalDateTime.now().plus(1, ChronoUnit.HOURS);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), futureTime);

        boolean beforeUnschedule = scheduledStreamingService.isVideoAvailable(testVideo.getId());
        assertFalse(beforeUnschedule, "Video trebalo bi da bude nedostupan pre otkazivanja");

        scheduledStreamingService.unscheduleVideo(testVideo.getId());

        boolean afterUnschedule = scheduledStreamingService.isVideoAvailable(testVideo.getId());
        assertTrue(afterUnschedule, "Video trebalo bi da bude dostupan nakon otkazivanja");

        System.out.println("✓ Zakazivanje je otkazano, video je sada dostupan");
    }

    /**
     * Test: Proslava aktivnog streaming-a - sve osobe gledaju istu minutažu
     */
    @Test
    @Transactional
    public void testMultipleViewersSameBroadcast() {
        LocalDateTime streamStartTime = LocalDateTime.now().minus(3, ChronoUnit.MINUTES);
        scheduledStreamingService.scheduleVideo(testVideo.getId(), streamStartTime);

        // Simulacija 3 gledalaca koji se javljaju u različitim vremenima
        Integer offset1 = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());

        try { Thread.sleep(500); } catch (InterruptedException e) {}

        Integer offset2 = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());

        try { Thread.sleep(500); } catch (InterruptedException e) {}

        Integer offset3 = scheduledStreamingService.getCurrentStreamOffset(testVideo.getId());

        // Svi offset-i trebalo bi da se povećavaju, ali sve osobe gledaju "živu" trenutnu minutažu
        assertTrue(offset1 < offset2, "Offset trebalo bi da se povećava");
        assertTrue(offset2 < offset3, "Offset trebalo bi da se povećava");

        System.out.println("✓ Sinhronizovani streaming - sve osobe prate živu emisiju:");
        System.out.println("  Gledaoc 1: " + offset1 + "s");
        System.out.println("  Gledaoc 2: " + offset2 + "s");
        System.out.println("  Gledaoc 3: " + offset3 + "s");
    }

    /**
     * Test: Greška - zakazivanje u prošlosti je odbijeno
     */
    @Test
    @Transactional
    public void testCannotScheduleInPast() {
        LocalDateTime pastTime = LocalDateTime.now().minus(1, ChronoUnit.HOURS);

        assertThrows(IllegalArgumentException.class, () -> {
            scheduledStreamingService.scheduleVideo(testVideo.getId(), pastTime);
        }, "Trebalo bi da bude bačena greška za zakazivanje u prošlosti");

        System.out.println("✓ Zakazivanje u prošlosti je pravilno odbijeno");
    }
}
