package com.rolemate.backend.controller;

import com.rolemate.backend.persistence.entity.FeedbackRecord;
import com.rolemate.backend.persistence.repository.FeedbackRecordRepository;
import com.rolemate.backend.persistence.repository.SessionRecordRepository;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for submitting post-session feedback.
 * This is a REST call (not WebSocket) because feedback is submitted after
 * the session ends, when the user may not have an active session context.
 */
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "${rolemate.websocket.allowed-origins:http://localhost:5173}")
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);
    private static final Set<String> VALID_RATINGS = Set.of("GOOD", "BAD");

    private final FeedbackRecordRepository feedbackRepository;
    private final SessionRecordRepository sessionRepository;

    public FeedbackController(FeedbackRecordRepository feedbackRepository,
                              SessionRecordRepository sessionRepository) {
        this.feedbackRepository = feedbackRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Accepts feedback for a completed session.
     * Request body: { "sessionId": "...", "rating": "GOOD" | "BAD" }
     */
    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String rating = body.get("rating");

        // Validate required fields
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "sessionId is required"));
        }

        if (rating == null || !VALID_RATINGS.contains(rating.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "rating must be GOOD or BAD"));
        }

        // Validate session exists
        if (!sessionRepository.existsById(sessionId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found: " + sessionId));
        }

        try {
            FeedbackRecord record = new FeedbackRecord(sessionId, "", rating.toUpperCase());
            feedbackRepository.save(record);

            log.info("Feedback recorded: sessionId={}, rating={}", sessionId, rating.toUpperCase());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "feedback recorded"));
        } catch (Exception e) {
            log.error("Failed to save feedback: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save feedback"));
        }
    }
}
