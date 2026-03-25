# RoleMate User Flow

## Primary User Journey

The initial RoleMate flow should remain short and predictable.

1. User opens the application.
2. User selects the role they are preparing for.
3. User clicks `Find Partner`.
4. System places the user in the queue for that role.
5. System finds another waiting user in the same role queue.
6. Both users receive a match confirmation.
7. A one-to-one chat session begins.
8. Either user can click `Next`.
9. Current session ends and the user returns to the queue.

## User Experience Principles

- no setup friction
- minimal waiting steps
- instant feedback when queueing
- obvious session state
- simple recovery when a partner leaves

## Session State Model

At a high level, a user moves through these states:

1. `idle`
2. `selecting_role`
3. `queued`
4. `matched`
5. `chatting`
6. `requesting_next`
7. `queued` again

## Key Interaction Rules

- users should only be matched with others in the same role category
- a user should not belong to more than one active queue or session at a time
- when a user clicks `Next`, the current session must be closed cleanly
- if a user disconnects, the partner should receive a clear session-ended event

## Example Flow

User A selects `Backend Engineering` and enters the queue.

User B selects `Backend Engineering` and enters the queue.

The system pairs:

`User A <-> User B`

They exchange messages. User A clicks `Next`. The system ends the session and returns User A to the backend queue. User B can either be re-queued automatically or shown a session-ended state depending on the UX decision taken during implementation.
