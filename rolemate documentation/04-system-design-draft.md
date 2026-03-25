# RoleMate System Design Draft

## Design Approach

Real systems should be built in this order:

1. problem clarity
2. user flow
3. system design
4. implementation

For RoleMate, this means validating matchmaking and session flow before introducing persistence, authentication, or WebRTC.

## Recommended Phase 1 Architecture

RoleMate Phase 1 should use a backend-first design centered on WebSockets and in-memory session state.

### Core Components

#### Frontend

A lightweight web client responsible for:

- role selection
- queue join action
- displaying match status
- sending and receiving chat messages
- requesting next partner

#### Backend

A Spring Boot service responsible for:

- WebSocket connection management
- role-based queues
- partner matching
- active session tracking
- message relay between matched users

## High-Level Architecture

```text
React Frontend
      |
      | WebSocket
      v
Spring Boot Backend
(Matchmaking + Session Management + Chat Relay)
```

## Core Backend Concepts

### Role Queue

The backend stores waiting users by role:

```text
Map<String, Queue<UserConnection>>
```

Example:

```text
backend -> [user1, user2]
frontend -> [user3]
```

### Session

An active match should be represented by a session object:

```text
Session
- sessionId
- userA
- userB
- role
- createdAt
- status
```

### WebSocket Events

Initial event contract can include:

- `JOIN_QUEUE`
- `MATCH_FOUND`
- `SEND_MESSAGE`
- `RECEIVE_MESSAGE`
- `NEXT_USER`
- `PARTNER_LEFT`
- `QUEUE_STATUS`

## Why No Database in Phase 1

Database design is not the priority for the first version.

Phase 1 can and should be built with in-memory storage because:

- it reduces setup cost
- it speeds up iteration
- it keeps the team focused on core logic
- it avoids premature schema design

Persistence becomes relevant only after the matchmaking loop proves stable and useful.

## Recommended Build Order

1. WebSocket server setup
2. queue management by role
3. session creation and cleanup
4. one-to-one chat relay
5. basic frontend integration
6. end-to-end testing
7. WebRTC signaling later

## Future Architecture Direction

After Phase 1 is stable, the system can evolve toward:

- WebRTC signaling support for video
- optional persistence for session history or analytics
- TURN/STUN integration for connectivity
- richer matching filters such as experience level and topics
