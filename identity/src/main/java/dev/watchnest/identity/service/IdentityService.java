package dev.watchnest.identity.service;

import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.identity.domain.InvalidCredentialsException;
import dev.watchnest.identity.domain.PasswordRules;
import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;
import dev.watchnest.identity.port.IdentityEventPublisher;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.identity.port.UserAccountRepository;
import dev.watchnest.identity.port.UserRegisteredV1;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class IdentityService {

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final IdentityEventPublisher events;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public IdentityService(
            UserAccountRepository accounts,
            PasswordHasher passwordHasher,
            IdentityEventPublisher events,
            Clock clock,
            Supplier<UUID> idGenerator
    ) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public AuthenticatedUser register(String rawUsername, String rawPassword) {
        Username username = Username.parse(rawUsername);
        PasswordRules.requireValid(rawPassword);

        String passwordHash = passwordHasher.hash(rawPassword);
        Instant createdAt = Instant.now(clock);
        UserAccount account = new UserAccount(idGenerator.get(), username, passwordHash, createdAt);

        accounts.insert(account);

        events.publish(new UserRegisteredV1(account.id(), username.value(), createdAt));
        return account.toAuthenticatedUser();
    }

    public AuthenticatedUser authenticate(String rawUsername, String rawPassword) {
        Username username = Username.parse(rawUsername);
        PasswordRules.requireValid(rawPassword);

        UserAccount account = accounts.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.verify(rawPassword, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return account.toAuthenticatedUser();
    }

    public Optional<AuthenticatedUser> findById(UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return accounts.findById(userId).map(UserAccount::toAuthenticatedUser);
    }
}
