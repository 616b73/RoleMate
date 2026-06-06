package com.rolemate.backend.websocket;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * Integration tests that start the full Spring Boot app and test
 * the WebSocket matchmaking flow end-to-end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.classformat.ignore=true")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketSession session1;
    private WebSocketSession session2;

    @AfterEach
    void tearDown() throws Exception {
        if (session1 != null && session1.isOpen()) session1.close();
        if (session2 != null && session2.isOpen()) session2.close();
    }

    private record TestClient(WebSocketSession session, BlockingQueue<String> messages) {}

    private TestClient connectClient() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        StandardWebSocketClient client = new StandardWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws/matchmaking");

        MessageCollector collector = new MessageCollector(messages);
        WebSocketSession session = client.execute(collector, new WebSocketHttpHeaders(), uri)
                .get(5, TimeUnit.SECONDS);

        return new TestClient(session, messages);
    }

    private JsonNode parseMessage(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    @Test
    void connect_receivesConnectedEvent() throws Exception {
        TestClient client = connectClient();
        session1 = client.session();

        String msg = client.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(msg);
        JsonNode node = parseMessage(msg);
        assertEquals("CONNECTED", node.get("type").asText());
    }

    @Test
    void joinQueue_receivesQueuedEvent() throws Exception {
        TestClient client = connectClient();
        session1 = client.session();
        client.messages().poll(5, TimeUnit.SECONDS); // CONNECTED

        session1.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}"));

        String msg = client.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(msg);
        JsonNode node = parseMessage(msg);
        assertEquals("QUEUED", node.get("type").asText());
        assertEquals("Backend Engineering", node.get("role").asText());
    }

    @Test
    void twoClients_sameRole_receiveMatchFound() throws Exception {
        TestClient c1 = connectClient();
        TestClient c2 = connectClient();
        session1 = c1.session();
        session2 = c2.session();

        c1.messages().poll(5, TimeUnit.SECONDS); // CONNECTED
        c2.messages().poll(5, TimeUnit.SECONDS); // CONNECTED

        session1.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_QUEUE\",\"role\":\"Data Science\"}"));
        c1.messages().poll(5, TimeUnit.SECONDS); // QUEUED

        session2.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_QUEUE\",\"role\":\"Data Science\"}"));
        c2.messages().poll(5, TimeUnit.SECONDS); // QUEUED

        String m1 = c1.messages().poll(5, TimeUnit.SECONDS);
        String m2 = c2.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(m1);
        assertNotNull(m2);
        assertEquals("MATCH_FOUND", parseMessage(m1).get("type").asText());
        assertEquals("MATCH_FOUND", parseMessage(m2).get("type").asText());
    }

    @Test
    void chatMessage_deliveredToPartner() throws Exception {
        TestClient c1 = connectClient();
        TestClient c2 = connectClient();
        session1 = c1.session();
        session2 = c2.session();

        c1.messages().poll(5, TimeUnit.SECONDS);
        c2.messages().poll(5, TimeUnit.SECONDS);

        session1.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_QUEUE\",\"role\":\"DevOps\"}"));
        c1.messages().poll(5, TimeUnit.SECONDS);

        session2.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_QUEUE\",\"role\":\"DevOps\"}"));
        c2.messages().poll(5, TimeUnit.SECONDS);

        c1.messages().poll(5, TimeUnit.SECONDS); // MATCH_FOUND
        c2.messages().poll(5, TimeUnit.SECONDS); // MATCH_FOUND

        session1.sendMessage(new TextMessage(
                "{\"type\":\"SEND_MESSAGE\",\"content\":\"Hello from user 1!\"}"));

        String received = c2.messages().poll(5, TimeUnit.SECONDS);
        assertNotNull(received);
        JsonNode node = parseMessage(received);
        assertEquals("RECEIVE_MESSAGE", node.get("type").asText());
        assertEquals("Hello from user 1!", node.get("content").asText());
    }
}
