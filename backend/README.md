# RoleMate Backend

Initial Spring Boot backend for the RoleMate MVP.

## Tech Stack

- Java 17+ compatible
- Spring Boot
- Maven
- Spring WebSocket

## Current Scope

This backend currently provides:

- HTTP health check at `/api/health`
- WebSocket endpoint at `/ws/matchmaking`
- in-memory role-based queues
- automatic one-to-one matching
- text message relay between matched users
- next partner flow for the MVP

## Expected Client Event Payloads

### Join Queue

```json
{
  "type": "JOIN_QUEUE",
  "role": "Backend Engineering"
}
```

### Send Chat Message

```json
{
  "type": "SEND_MESSAGE",
  "content": "Tell me about your recent backend project."
}
```

### Next Partner

```json
{
  "type": "NEXT_USER"
}
```

## Example Server Events

- `CONNECTED`
- `QUEUED`
- `MATCH_FOUND`
- `RECEIVE_MESSAGE`
- `SESSION_ENDED`
- `PARTNER_LEFT`
- `ERROR`

## Run

Once Maven is available in the environment:

```bash
mvn spring-boot:run
```

Or package it with:

```bash
mvn clean package
```

## Notes

State is intentionally stored in memory for Phase 1. This keeps the implementation focused on matchmaking and chat instead of persistence concerns.
