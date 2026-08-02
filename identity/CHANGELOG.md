# Identity Changelog

Changelog for the `identity` module.

## [0.1.0] - 2026-08-02

### Added

- Username rules (canonical lower-case, length, allowed characters)
- `UserAccount` / `AuthenticatedUser` and register/authenticate use cases
- Ports: `PasswordHasher`, `UserAccountRepository`, `IdentityEventPublisher`
- Credential-free `UserRegisteredV1` registration event
- In-memory `UserAccountRepository` for tests and early wiring
- Module README
