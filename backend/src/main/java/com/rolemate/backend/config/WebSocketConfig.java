package com.rolemate.backend.config;

import com.rolemate.backend.websocket.RoleMateWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoleMateWebSocketHandler roleMateWebSocketHandler;

    public WebSocketConfig(RoleMateWebSocketHandler roleMateWebSocketHandler) {
        this.roleMateWebSocketHandler = roleMateWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roleMateWebSocketHandler, "/ws/matchmaking")
                .setAllowedOriginPatterns("*");
    }
}
