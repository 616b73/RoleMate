# RoleMate Backend

Spring Boot backend service for the RoleMate matchmaking and communication platform.

## Tech Stack

- Java 25
- Spring Boot 3.5.0
- Spring WebSocket (raw handler)
- Spring Data JDBC (lightweight, no Hibernate ORM)
- PostgreSQL 18 with Flyway migrations
- Maven

## Current Scope

This backend provides:

- HTTP health check at `/api/health` (includes live + historical metrics)
- Queue status at `/api/queue/status`
- Supported roles list at `/api/roles`
- WebSocket endpoint at `/ws/matchmaking`
- In-memory role-based matchmaking queues
- Automatic one-to-one matching
- Text message relay between matched users
- WebRTC signaling relay (SDP offers/answers, ICE candidates)
- Video readiness notifications
- Next partner flow
- Session metadata persistence to PostgreSQL

## WebSocket Event Protocol

### Client → Server Events

#### Matchmaking

```json
{"type": "JOIN_QUEUE", "role": "Backend Engineering"}
{"type": "SEND_MESSAGE", "content": "Tell me about your recent project."}
{"type": "NEXT_USER"}
```

#### WebRTC Signaling

```json
{"type": "WEBRTC_OFFER", "sdp": "<SDP offer string>"}
{"type": "WEBRTC_ANSWER", "sdp": "<SDP answer string>"}
{"type": "ICE_CANDIDATE", "candidate": "<candidate>", "sdpMid": "<mid>", "sdpMLineIndex": 0}
{"type": "VIDEO_READY"}
{"type": "VIDEO_ENDED"}
```

### Server → Client Events

- `CONNECTED` — connection established
- `QUEUED` — added to role queue
- `MATCH_FOUND` — matched with a partner
- `RECEIVE_MESSAGE` — incoming chat message
- `SESSION_ENDED` — session ended (by you)
- `PARTNER_LEFT` — partner disconnected or clicked Next
- `ERROR` — error message
- `WEBRTC_OFFER` — relayed SDP offer from partner
- `WEBRTC_ANSWER` — relayed SDP answer from partner
- `ICE_CANDIDATE` — relayed ICE candidate from partner
- `PARTNER_VIDEO_READY` — partner's camera is ready
- `PARTNER_VIDEO_ENDED` — partner stopped video

## Database Setup

The app uses PostgreSQL. Tables are managed by Flyway migrations automatically on startup.

### Prerequisites

1. PostgreSQL running on `localhost:5432`
2. Database `rolemate` created
3. User with access credentials

### Configuration

Database connection is configured in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rolemate
spring.datasource.username=<your-user>
spring.datasource.password=<your-password>
```

These can be overridden via environment variables for containerized deployments:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/rolemate
SPRING_DATASOURCE_USERNAME=app_user
SPRING_DATASOURCE_PASSWORD=secret
```

## Run

```bash
mvn spring-boot:run
```

Or package and run the JAR:

```bash
mvn clean package
java -jar target/rolemate-backend-0.0.1-SNAPSHOT.jar
```

## Tests

Tests use H2 in-memory database (no PostgreSQL required):

```bash
mvn clean test
```

## Architecture Notes

- **In-memory state** remains the source of truth for live matchmaking and active sessions
- **PostgreSQL** stores session metadata (who matched, when, duration, role) for analytics
- **Chat messages are ephemeral** — not stored in the database by design
- **WebRTC signaling** is relay-only — the backend forwards SDP/ICE between matched partners; video streams flow peer-to-peer
