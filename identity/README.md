# identity

Java library: user accounts, credential hashing ports, and authentication use cases.

**Version:** `identityVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- register and authenticate users by username and password;
- expose safe identity results (id + username) without credential leakage;
- publish credential-free registration facts for integration adapters.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Username | `Username.parse`: trim, `Locale.ROOT` lower-case, length 3–32, `[a-z0-9._-]` |
| Account | `UserAccount`: id, username, password hash, createdAt (internal) |
| Result | `AuthenticatedUser`: id + canonical username only |
| Passwords | `PasswordHasher` port; length 8 code points … 72 UTF-8 bytes; never trim |
| Storage | `UserAccountRepository.insert` enforces atomic username uniqueness |
| Events | `UserRegisteredV1` via `IdentityEventPublisher` after successful register |
| Use cases | `IdentityService`: `register`, `authenticate`, `findById` |

## Public surface for `planner-app`

```text
IdentityService
  register(rawUsername, rawPassword) -> AuthenticatedUser
  authenticate(rawUsername, rawPassword) -> AuthenticatedUser
  findById(userId) -> Optional<AuthenticatedUser>

AuthenticatedUser { UUID id, String username }

Ports (runtime adapters in planner-app, except optional in-memory repo):
  PasswordHasher
  UserAccountRepository
  IdentityEventPublisher

Event:
  UserRegisteredV1 { UUID userId, String username, Instant occurredAt }
```

Authenticated `id` is the UUID that planner must use as `LibraryProfile.id` /
`WatchEvent.ownerId`.

## Error categories

| Exception | Meaning |
| --- | --- |
| `InvalidUsernameException` | username format/length invalid |
| `InvalidPasswordException` | password length invalid (password not modified) |
| `DuplicateUsernameException` | username already taken (case-insensitive) |
| `InvalidCredentialsException` | unknown user or wrong password (generic message) |

## Invariants

- No plaintext password storage, return, or event payload.
- No password hash on `AuthenticatedUser` or `UserRegisteredV1`.
- Does not own library profiles, quotas, or watch events.

## Constraints

- JDK only (no Spring, HTTP, or session types in this module).
- Domain and ports without I/O frameworks; persistence is a port.
- JVM unit tests (no container or network).

## Layout

```text
identity/
  src/main/java/dev/watchnest/identity/
    domain/
    port/
    service/
    adapter/memory/
  src/test/java/...
  build.gradle.kts
  gradle.properties
  CHANGELOG.md
  README.md
```

## Build and test

Requires root `settings.gradle.kts` to `include("identity")`.

```bash
./gradlew :identity:test
./gradlew :identity:build
```

Windows:

```powershell
.\gradlew.bat :identity:test
```

## Scope (0.1.0)

In scope: username rules, register/authenticate, password hash port, in-memory
repository for tests/early wiring, registration event port.

Out of scope: OAuth, email verification/reset, durable account persistence
(beyond in-memory).
