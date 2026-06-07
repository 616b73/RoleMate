package com.rolemate.backend.controller;

import com.rolemate.backend.matchmaking.service.MatchmakingService;
import com.rolemate.backend.persistence.service.PersistenceService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint that provides basic service status
 * along with live metrics (connected users, active sessions)
 * and historical data from the database.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final MatchmakingService matchmakingService;
    private final PersistenceService persistenceService;

    public HealthController(MatchmakingService matchmakingService, PersistenceService persistenceService) {
        this.matchmakingService = matchmakingService;
        this.persistenceService = persistenceService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "RoleMate Backend");
        response.put("timestamp", Instant.now().toString());
        response.put("connectedUsers", matchmakingService.getConnectedUserCount());
        response.put("activeSessions", matchmakingService.getActiveSessionCount());
        response.put("totalSessionsRecorded", persistenceService.getTotalSessionCount());
        response.put("completedSessions", persistenceService.getCompletedSessionCount());
        return response;
    }
}
