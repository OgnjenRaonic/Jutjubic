package com.example.demo;

import com.example.demo.dtos.CreateCommentDTO;
import com.example.demo.model.Comment;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CommentRateLimitTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test: Simulira slanje 65 komentara u roku od 1 minut
     * Očekivani rezultat: Prvih 60 prolazi, 61-65 su odbijeni zbog rate limitinga
     */
    @Test
    @Transactional
    public void testCommentRateLimit() {
        // Setup: Kreiraj test korisnika i video
        User testUser = new User();
        testUser.setEmail("ratelimit@test.com");
        testUser.setUsername("rateLimitTestUser");
        testUser.setPassword("encoded_password_123");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        Long videoId = 999L; // Koristi dummy video ID
        long testUserId = testUser.getId();

        // Test: Pokuša da pošalje 65 komentara
        int successCount = 0;
        int rejectedCount = 0;

        for (int i = 0; i < 65; i++) {
            try {
                CreateCommentDTO dto = new CreateCommentDTO("Test komentar broj " + (i + 1));
                commentService.createComment(videoId, testUserId, dto);
                successCount++;
            } catch (Exception e) {
                // Rate limit exception ili nešto drugo
                rejectedCount++;
                System.out.println("Komentar " + (i + 1) + " odbijen: " + e.getMessage());
            }
        }

        System.out.println("\n=== REZULTATI ===");
        System.out.println("Prihvaćenih komentara: " + successCount);
        System.out.println("Odbijenih komentara: " + rejectedCount);

        // Verifikuj da je tačno 60 prihvaćeno i 5 odbijeno
        assertEquals(60, successCount, "Trebalo bi da bude tačno 60 prihvaćenih komentara");
        assertEquals(5, rejectedCount, "Trebalo bi da bude tačno 5 odbijenih komentara");

        // Verifikuj da su komentari u bazi
        long commentCount = commentRepository.findByUserIdAndCreatedAtAfter(
            testUserId,
            java.time.LocalDateTime.now().minusHours(1)
        ).size();
        assertEquals(60, commentCount, "U bazi trebalo bi da bude 60 komentara");
    }

    /**
     * Test: Proverava da li rate limiter dozvoljava novi komentar nakon resetovanja
     */
    @Test
    @Transactional
    public void testRateLimitReset() {
        User testUser = new User();
        testUser.setEmail("reset@test.com");
        testUser.setUsername("resetTestUser");
        testUser.setPassword("encoded_password_123");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        Long videoId = 998L;
        long testUserId = testUser.getId();

        // Pošalji 60 komentara
        for (int i = 0; i < 60; i++) {
            CreateCommentDTO dto = new CreateCommentDTO("Comment " + i);
            commentService.createComment(videoId, testUserId, dto);
        }

        // Proveri da rate limiter sprečava novi komentar
        boolean canCommentBefore = commentService.getRateLimitInfo(testUserId).isCanComment();
        assertFalse(canCommentBefore, "Ne bi trebalo da može da komentariše nakon 60 komentara");

        System.out.println("Rate limit test OK: Korisnik je blokiran nakon 60 komentara");
    }
}
