# RoleMate Roadmap

## Delivery Principle

Build the product in layers. Do not start with the hardest technology or the broadest feature set.

The correct progression for RoleMate is:

`backend foundations -> matchmaking -> chat -> frontend integration -> video later`

## Practical Execution Plan

### Step 1: Define the MVP

Lock the first version to this flow only:

- user selects role
- user clicks `Find Partner`
- system matches the user
- chat begins
- user can click `Next`

Do not add video, auth, or advanced UX before this loop works.

### Step 2: Finalize the User Flow

Document the exact interaction flow before coding:

1. user opens app
2. user selects role
3. user clicks start
4. user enters queue
5. system finds another user
6. both users connect
7. chat starts
8. user clicks next
9. user returns to queue

If the flow is clear, implementation decisions become easier.

### Step 3: Build the Backend First

Start with Spring Boot and WebSockets.

Initial implementation goals:

- accept socket connections
- store users in role-based queues
- match users when queue size reaches two
- create sessions
- forward chat messages to the matched partner

At this stage, use Postman WebSocket support or a temporary test client to validate behavior.

### Step 4: Build a Minimal Frontend

Only after backend behavior is working, build a very simple frontend that can:

- connect to the WebSocket server
- select a role
- join the queue
- display chat messages
- trigger next partner

The UI should be intentionally plain in the first cut.

### Step 5: Test End-to-End

Validate:

- queueing works
- matches happen correctly
- sessions clean up properly
- next partner works without leaks or stale state

### Step 6: Add Video Later

Only after matchmaking and chat are reliable:

- add camera and microphone handling
- implement signaling through the backend
- establish peer-to-peer WebRTC connections

This should be treated as a separate phase, not part of initial MVP delivery.

## Phased Roadmap

### Phase 1: Basic Matchmaking

- WebSocket server
- role-based queues
- session management
- text chat
- next partner behavior

### Phase 2: Video Communication

- signaling support
- WebRTC integration
- peer-to-peer video sessions
- disconnect and reconnect handling

### Phase 3: Enhanced Matching

- role refinements
- experience filters
- topic preferences
- better queueing logic

### Phase 4: User Experience

- session timer
- partner feedback
- cleaner UI
- stronger onboarding and state messaging

## Common Mistakes To Avoid

- starting with WebRTC
- designing a database before validating product flow
- building frontend before backend behavior exists
- adding too many features before the core loop works

## Immediate Next Actions

The most practical next tasks are:

1. create the Spring Boot project
2. add WebSocket configuration
3. implement `JOIN_QUEUE`
4. match two users from the same role queue
5. verify the session loop with a basic test client

That sequence will produce real progress instead of cosmetic progress.
