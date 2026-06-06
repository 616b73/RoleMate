package com.rolemate.backend.controller;

import com.rolemate.backend.matchmaking.model.RoleCategory;
import com.rolemate.backend.matchmaking.service.MatchmakingService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for queue status and role discovery.
 * Useful for frontend to display available roles and live queue sizes.
 */
@RestController
@RequestMapping("/api")
public class QueueController {

    private final MatchmakingService matchmakingService;

    public QueueController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    /**
     * Returns current queue sizes per role and the count of active sessions.
     *
     * <pre>
     * GET /api/queue/status
     * {
     *   "queues": { "Backend Engineering": 2, "Frontend Engineering": 0, ... },
     *   "activeSessions": 3
     * }
     * </pre>
     */
    @GetMapping("/queue/status")
    public Map<String, Object> queueStatus() {
        return Map.of(
                "queues", matchmakingService.getQueueStatus(),
                "activeSessions", matchmakingService.getActiveSessionCount()
        );
    }

    /**
     * Returns the list of supported role categories.
     *
     * <pre>
     * GET /api/roles
     * ["Backend Engineering", "Frontend Engineering", ...]
     * </pre>
     */
    @GetMapping("/roles")
    public List<String> supportedRoles() {
        return Arrays.stream(RoleCategory.values())
                .map(RoleCategory::getDisplayName)
                .toList();
    }
}
