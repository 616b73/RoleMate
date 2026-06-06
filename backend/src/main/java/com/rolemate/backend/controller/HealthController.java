package com.rolemate.backend.controller;

import com.rolemate.backend.matchmaking.service.MatchmakingService;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint that provides basic service status
 * along with live metrics (connected users, active sessions).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final MatchmakingService matchmakingService;

    public HealthController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "RoleMate Backend",
                "timestamp", Instant.now().toString(),
                "connectedUsers", matchmakingService.getConnectedUserCount(),
                "activeSessions", matchmakingService.getActiveSessionCount()
        );
    }
}
