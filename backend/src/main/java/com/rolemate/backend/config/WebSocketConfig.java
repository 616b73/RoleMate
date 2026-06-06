package com.rolemate.backend.config;

import com.rolemate.backend.websocket.RoleMateWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the RoleMate WebSocket handler at /ws/matchmaking with configurable
 * allowed-origin patterns sourced from application.properties.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoleMateWebSocketHandler roleMateWebSocketHandler;
    private final String allowedOrigins;

    public WebSocketConfig(RoleMateWebSocketHandler roleMateWebSocketHandler,
                           @Value("${rolemate.websocket.allowed-origins:*}") String allowedOrigins) {
        this.roleMateWebSocketHandler = roleMateWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addHandler(roleMateWebSocketHandler, "/ws/matchmaking")
                .setAllowedOriginPatterns(origins);
    }
}
