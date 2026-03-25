# RoleMate MVP Definition

## MVP Goal

The first release of RoleMate should validate the core matchmaking experience, not the full product vision.

The MVP is successful if two users preparing for the same role can quickly find each other and exchange messages in a stable session.

## Exact MVP Scope

The initial version includes only:

- role selection
- join matchmaking queue
- automatic role-based pairing
- simple one-to-one text chat
- next partner action

## Explicit Non-Goals

The following are intentionally out of scope for the MVP:

- video calls
- authentication
- user profiles
- database persistence
- fancy UI
- advanced filters
- feedback systems
- analytics dashboards

## Why the MVP Is Narrow

This project depends on one core capability: matchmaking.

If matching is slow, unreliable, or confusing, the product fails regardless of how polished the rest of the system looks. For that reason, the MVP should isolate and validate the most important real-time workflow before adding complexity.

## MVP User Story

As a candidate preparing for a specific role, I want to instantly find another candidate preparing for the same role so that I can practice interview conversations without manual coordination.

## MVP Success Criteria

The MVP should satisfy the following:

- a user can choose a role and join the queue
- the server can store waiting users by role
- when two compatible users are available, the system pairs them
- matched users can exchange text messages in real time
- either user can leave the current session and request the next partner

## Recommended Technical Constraint

Phase 1 should use in-memory state only.

Suggested examples:

- `Map<String, Queue<UserConnection>>` for waiting users by role
- `Map<String, Session>` for active pair sessions

This keeps the project focused on real-time behavior instead of premature persistence design.

## MVP Design Principle

Build the smallest usable loop:

`match -> chat -> next`

Everything else comes later.
