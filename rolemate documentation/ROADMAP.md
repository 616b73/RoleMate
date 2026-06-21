# RoleMate Roadmap & Developer Guide

## Architecture & System Flow
The application uses a Spring Boot backend over WebSockets. State is primarily in-memory for live matchmaking, with metadata persisted asynchronously to PostgreSQL.

**Current Interaction Flow:**
1. User connects via WebSocket (`/ws/matchmaking`).
2. User joins a queue based on their role (`JOIN_QUEUE`).
3. The system automatically matches users in the same queue. Both receive a `MATCH_FOUND` event.
4. Users communicate via text (`SEND_MESSAGE` / `RECEIVE_MESSAGE`).
5. Users can initiate a video call via WebRTC signaling (`WEBRTC_OFFER` → `WEBRTC_ANSWER` → `ICE_CANDIDATE`).
6. A user can skip and request a new partner (`NEXT_USER`) or leave.
7. The session ends, metadata is persisted to PostgreSQL, and users return to the queue.

## Tech Stack
- **Backend Core**: Spring Boot 3.5.0, Java 25
- **Real-Time Communication**: Spring WebSocket (raw text handler)
- **Video/Audio**: WebRTC (peer-to-peer media, signaling relayed via WebSocket)
- **Database**: PostgreSQL 18, Spring Data JDBC, Flyway (for schema migrations)
- **Build / Testing**: Maven, JUnit 5, Mockito

---

## Development Log (Chronological)

This section documents every major milestone in chronological order.
Each entry records what was built, why certain decisions were made, and what was learned.

---

### Milestone 1: Project Definition & Initial Setup

**Goal:** Establish the project vision, define the MVP scope, and set up the repository structure.

**What was done:**
- Created the repository and initial project scaffolding.
- Documented the product vision: an Omegle-style platform for interview practice where users match by role.
- Defined the MVP scope explicitly — role selection, queue-based matching, text chat, next partner flow.
- Documented explicit non-goals for MVP: no video, no auth, no database, no fancy UI.
- Created architecture draft: backend-first with Spring Boot and WebSockets, in-memory state only.
- Defined the delivery principle: "build in layers, validate the core loop before expanding."

**Key decisions:**
- **Backend-first approach** — get the matchmaking logic working before touching any frontend.
- **In-memory state only** for Phase 1 — avoids premature database design and keeps iteration fast.
- **No authentication** — the product is anonymous by design. Identity adds friction and isn't needed until user profiles exist.

---

### Milestone 2: Base Matchmaking MVP (`9517725`)

**Goal:** Build the first working version of the matchmaking loop — connect, queue, match, chat, next.

**What was done:**
- Created the Spring Boot application with WebSocket support.
- Implemented `RoleMateWebSocketHandler` as the raw WebSocket handler.
- Defined the message protocol with typed events:
  - Client → Server: `JOIN_QUEUE`, `SEND_MESSAGE`, `NEXT_USER`
  - Server → Client: `CONNECTED`, `QUEUED`, `MATCH_FOUND`, `RECEIVE_MESSAGE`, `SESSION_ENDED`, `PARTNER_LEFT`, `ERROR`
- Built in-memory models: `MatchSession`, `UserConnection`, `ClientEvent`, `ServerEvent`.
- Built a monolithic `MatchmakingService` handling queue management, matching, chat relay, and session cleanup.
- Added `HealthController` with a basic health check at `/api/health`.

**What the code could do at this point:**
- Accept WebSocket connections.
- Place users into role-based queues.
- Match two users in the same queue.
- Relay text messages between matched users.
- Handle "next partner" and disconnect cleanup.

**Known issues at this stage:**
- All matchmaking logic was in a single service (monolithic design).
- `synchronized` blocks used for concurrency — coarse-grained, potential bottleneck.
- No input validation — any role string was accepted.
- No logging — hard to debug matchmaking behavior.
- No automated tests.

---

### Milestone 3: Architecture Refactor & Validation (`310d982`)

**Goal:** Restructure the monolithic backend into a clean, layered, testable architecture.

**What was done:**
- **Service layer separation:** Extracted `SessionService` from `MatchmakingService`. SessionService owns session storage maps and provides create/end/query operations. MatchmakingService focuses on queues, matching, and message relay.
- **Fine-grained concurrency:** Replaced global `synchronized` methods with per-role `ReentrantLock`. Each role queue gets its own lock, so matching in "Backend Engineering" doesn't block matching in "Data Science". A separate global lock handles disconnect cleanup (cross-role operation).
- **Input validation:** Created `RoleCategory` enum with 6 supported roles and case-insensitive `fromDisplayName()` lookup. Invalid roles are rejected with a clear error listing valid options.
- **Jackson hardening:** Configured `@JsonIgnoreProperties(ignoreUnknown = false)` on `ClientEvent` to reject unknown fields. Added `JacksonConfig` with `JavaTimeModule` for `Instant` serialization and null suppression.
- **Monitoring APIs:** Created `QueueController` exposing `GET /api/queue/status` (per-role queue sizes) and `GET /api/roles` (supported role list). Updated `HealthController` to report live metrics (connectedUsers, activeSessions).
- **Logging:** Added SLF4J structured logging at every decision point — connection registered, queue joined, match found, session ended, user disconnected.
- **WebSocket config:** Made allowed origins configurable via `application.properties` (`rolemate.websocket.allowed-origins`).

**Testing suite added:**
- `RoleCategoryTest` (6 tests) — exact/case-insensitive/whitespace/unknown/null lookups.
- `SessionServiceTest` (9 tests) — create, query, end, double-end, count, partner lookup.
- `MatchmakingServiceTest` (11 tests) — connect, queue, match, chat, next, disconnect, monitoring.
- `WebSocketIntegrationTest` (4 tests) — full E2E: connect → queue → match → chat via real WebSocket.
- Total: **36 tests, all passing.**

**Build environment fixes:**
- Environment had JRE only (no `javac`). Installed `openjdk-25-jdk-headless`.
- Spring Boot 3.2.5's compiler plugin didn't support Java 25. Bumped to Spring Boot 3.5.0 + `maven-compiler-plugin 3.14.0`.
- Mockito on Java 25 needed `--add-opens` JVM args in surefire config.
- Spring Boot integration tests hit class format errors with Java 25 — added `spring.classformat.ignore=true` and extracted anonymous inner classes to named classes.

**Key decisions:**
- **Per-role locks over global synchronized** — better throughput when multiple roles are matching concurrently.
- **`RoleCategory` enum over free-text roles** — prevents typos and normalizes input. Easy to extend later.
- **Strict JSON parsing** — catches client bugs early rather than silently ignoring unknown fields.

**Package structure at this point:**
```
com.rolemate.backend
├── config/         (JacksonConfig, WebSocketConfig)
├── controller/     (HealthController, QueueController)
├── exception/      (RoleMateException)
├── matchmaking/
│   ├── model/      (ClientEvent, ClientEventType, ServerEvent, ServerEventType,
│   │                MatchSession, RoleCategory, UserConnection)
│   └── service/    (MatchmakingService, SessionService)
└── websocket/      (RoleMateWebSocketHandler)
```

---

### Milestone 4: Database Persistence & WebRTC Signaling

**Goal:** Add session metadata persistence (PostgreSQL) and WebRTC signaling relay for future video calls.

#### Part A: Database Persistence

**What was done:**
- Added PostgreSQL connectivity via Spring Data JDBC (NOT Hibernate/JPA).
- Added Flyway for schema migration management.
- Created `match_sessions` table with: id, role, user_a_id, user_b_id, session_type, status, created_at, ended_at, duration_seconds.
- Created `SessionRecord` (JDBC entity), `SessionRecordRepository` (with custom count queries), and `PersistenceService` (async bridge).
- Wired `PersistenceService` into `SessionService` — on create and end, session metadata is written to DB asynchronously.
- Updated `HealthController` to include `totalSessionsRecorded` and `completedSessions` from the database.
- Enabled `@EnableAsync` on the main application class.
- Tests use H2 in-memory database in PostgreSQL compatibility mode — no external DB needed to run `mvn test`.

**Why Spring Data JDBC instead of Hibernate/JPA:**
- Our use case is a single table with simple CRUD — no entity relationships, no lazy loading, no cascades needed.
- Spring Data JDBC maps directly to SQL without proxy objects, L1/L2 caches, or entity lifecycle complexity.
- It's easier to reason about: what you write is what gets executed. No "magic" behind the scenes.
- Still gives us the `Repository` interface convenience for CRUD + custom queries.

**Why async writes:**
- DB writes must never block real-time WebSocket flows. A slow INSERT should not delay a `MATCH_FOUND` event.
- `@Async` methods run on a separate thread pool. Failures are caught and logged — they don't crash the matchmaking loop.

**Why metadata only (no chat messages):**
- Chat messages are ephemeral by design — they disappear when the session ends, like Omegle.
- Storing messages adds storage cost, privacy concerns, and complexity with no Phase 2 use case.
- Session metadata (who matched, when, how long, which role) is sufficient for analytics.

**Database config externalization:**
- All DB connection params are in `application.properties` with Spring's standard property names.
- Can be overridden via environment variables (`SPRING_DATASOURCE_URL`, etc.) for future containerization.
- Tests have their own `src/test/resources/application.properties` pointing to H2.

**Schema migration (Flyway):**
```sql
CREATE TABLE match_sessions (
    id                VARCHAR(36)  PRIMARY KEY,
    role              VARCHAR(50)  NOT NULL,
    user_a_id         VARCHAR(100) NOT NULL,
    user_b_id         VARCHAR(100) NOT NULL,
    session_type      VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL,
    ended_at          TIMESTAMP,
    duration_seconds  BIGINT
);
```

#### Part B: WebRTC Signaling

**What was done:**
- Added 5 new client event types: `WEBRTC_OFFER`, `WEBRTC_ANSWER`, `ICE_CANDIDATE`, `VIDEO_READY`, `VIDEO_ENDED`.
- Added 5 new server event types: `WEBRTC_OFFER`, `WEBRTC_ANSWER`, `ICE_CANDIDATE`, `PARTNER_VIDEO_READY`, `PARTNER_VIDEO_ENDED`.
- Extended `ClientEvent` and `ServerEvent` with signaling fields: `sdp`, `candidate`, `sdpMid`, `sdpMLineIndex`.
- Created `SignalingService` in new `com.rolemate.backend.signaling.service` package — handles relay of SDP offers/answers and ICE candidates between matched partners.
- Wired SignalingService into `MatchmakingService.handleEvent()` switch statement.
- All 36 tests pass after the changes.

**How WebRTC signaling works (for learning reference):**
1. User A wants to start a video call with their matched partner (User B).
2. User A creates an SDP offer (describes their media capabilities) and sends `WEBRTC_OFFER` to the server.
3. The server relays the SDP offer to User B via their WebSocket connection.
4. User B creates an SDP answer and sends `WEBRTC_ANSWER` back through the server.
5. Both users exchange ICE candidates (network connectivity information) via `ICE_CANDIDATE` events.
6. Once ICE negotiation completes, a direct peer-to-peer connection is established between browsers.
7. Video/audio streams flow directly between browsers — **the server never touches the media**.

**Key design decision — relay only:**
- The backend is a signaling server, not a media server. It just forwards handshake messages.
- This means zero bandwidth cost for video on the backend, and the architecture scales without media infrastructure.
- For users behind restrictive NATs/firewalls, a TURN server would be needed later (not implemented yet).

#### Documentation Cleanup
- Consolidated the 5 scattered numbered docs (01-product-overview.md through 05-roadmap.md) into two clear files: `OVERVIEW.md` (static product context) and `ROADMAP.md` (living development log).
- Updated root `README.md` to be a concise "how to run" guide.
- Updated `backend/README.md` with full WebSocket event protocol, DB setup instructions, and architecture notes.

**Package structure at this point:**
```
com.rolemate.backend
├── config/            (JacksonConfig, WebSocketConfig)
├── controller/        (HealthController, QueueController)
├── exception/         (RoleMateException)
├── matchmaking/
│   ├── model/         (ClientEvent, ClientEventType, ServerEvent, ServerEventType,
│   │                   MatchSession, RoleCategory, UserConnection)
│   └── service/       (MatchmakingService, SessionService)
├── persistence/
│   ├── entity/        (SessionRecord)
│   ├── repository/    (SessionRecordRepository)
│   └── service/       (PersistenceService)
├── signaling/
│   └── service/       (SignalingService)
└── websocket/         (RoleMateWebSocketHandler)
```

---

### Milestone 5: Frontend Integration & WebRTC Client

**Goal:** Build a user-facing client to consume the matchmaking API and establish real-time video calls.

**What was done:**
- Scaffolded a new React application using Vite (`frontend/`).
- Connected the UI to the backend WebSocket API.
- Implemented role selection and queue joining.
- Developed `ChatScreen` for text-based messaging between matched peers.
- Built a robust `useWebRTC` hook to manage RTCPeerConnection, ICE candidates, and local/remote media streams.
- Integrated `VideoPanel` to display the video call seamlessly above the chat interface.
- Verified the end-to-end flow using browser testing: successful matching, video establishment, concurrent text chat, and clean disconnections.

**Key decisions:**
- **React + Vite** — standard modern stack for fast frontend development.
- **Custom `useWebRTC` hook** — encapsulates all the complex WebRTC event handling and state management away from the UI components.
- **Seamless Video/Chat Integration** — text chat remains functional during video calls, maintaining the core MVP experience while adding media capabilities.

---

### Milestone 6: UX Polish & Session Experience

**Goal:** Transform the raw matchmaking loop into a polished user experience with proper state transitions, audio feedback, error recovery, and post-session feedback.

**What was done:**

**Feature 1 — LEAVE_QUEUE event:**
- Added `LEAVE_QUEUE` client event and `QUEUE_LEFT` server event to the protocol.
- Implemented `leaveQueue()` handler in `MatchmakingService` — removes user from queue without disconnecting.
- Fixed the frontend Cancel button which previously used `window.location.reload()` — now sends a proper WebSocket event.
- Added test verifying queue removal and that the user remains connected.

**Feature 2 — Session Timer:**
- Created `SessionTimer` component using `useEffect` + `setInterval` — counts up from 00:00 in MM:SS format.
- Rendered in the chat header between role badge and action buttons.
- Timer starts on `MATCH_FOUND` and resets on next partner/session end.
- Uses `font-variant-numeric: tabular-nums` for stable width during counting.

**Feature 3 — Notification Sounds:**
- Created `useSoundEffects` hook using the Web Audio API — no external audio files needed.
- `playMatchSound()` — ascending C5→E5 two-tone chime on match found.
- `playMessageSound()` — subtle A5 pop on message received.
- Gain envelope (fade in/out) prevents audio clicks.

**Feature 4 — Connection Error Overlay:**
- Created `ConnectionOverlay` component — full-screen semi-transparent overlay with backdrop blur.
- Shows "Connection Lost" with auto-reconnect countdown and manual "Reconnect Now" button.
- Exposed `reconnect()` function from `useWebSocket` hook.
- Replaced the old minimal connection-bar banner.

**Feature 5 — End-of-Session Feedback:**
- Created Flyway V2 migration: `session_feedback` table (FK to `match_sessions`, stores GOOD/BAD rating).
- Created `FeedbackRecord` entity and `FeedbackRecordRepository`.
- Created `FeedbackController` REST endpoint (`POST /api/feedback`) with validation.
- Created `FeedbackScreen` component — shown between session end and role selection.
- Displays session role badge, duration, thumbs up/down buttons, and skip option.
- Posts feedback via HTTP (not WebSocket) since it's post-session.
- Updated app state machine: CHAT → FEEDBACK → ROLE_SELECT.

**Key decisions:**
- **Web Audio API over audio files** — zero network requests, no asset management, works offline.
- **REST for feedback** — the WebSocket session context may not map to the match session after it ends, so a stateless POST is cleaner.
- **Ref pattern for sounds/signaling** — avoids stale closures in `useCallback` without adding dependencies that would cause re-renders.
- **LEAVE_QUEUE as a first-class event** — the `window.location.reload()` hack was unacceptable; proper protocol events keep the WebSocket connection alive.

**Database schema at this point:**
```sql
-- V1
match_sessions (id, role, user_a_id, user_b_id, session_type, status, created_at, ended_at, duration_seconds)

-- V2
session_feedback (id SERIAL, session_id FK, user_id, rating, created_at)
```

**Tests:** 37 backend tests passing (36 existing + 1 new LEAVE_QUEUE test).

---

## Next Steps

### 🚧 Immediate Objectives
- **STUN/TURN Configuration:** Evaluate TURN server options for restrictive network fallback (STUN is already configured with Google's public servers).
- **Containerization:** Dockerfile for backend + frontend, Docker Compose for full stack with PostgreSQL.

### ⏳ Future Enhancements
- **Enhanced Matching:** Sub-category roles, experience level filters (junior/mid/senior), topic preferences (system design vs. behavioral vs. DSA).
- **User Experience:** Onboarding flow, typing indicators, read receipts.
- **Production Readiness:** CI/CD pipeline, rate limiting, observability/metrics.
- **Authentication:** Optional accounts and persistent user identity (when features require it).

