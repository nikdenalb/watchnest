package dev.watchnest.identity.service;

import dev.watchnest.identity.adapter.memory.InMemoryUserAccountRepository;
import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.identity.domain.DuplicateUsernameException;
import dev.watchnest.identity.domain.InvalidCredentialsException;
import dev.watchnest.identity.domain.InvalidPasswordException;
import dev.watchnest.identity.domain.InvalidUsernameException;
import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;
import dev.watchnest.identity.port.UserRegisteredV1;
import dev.watchnest.identity.support.RecordingIdentityEventPublisher;
import dev.watchnest.identity.support.RecordingPasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-02T15:00:00Z");
    private static final UUID FIXED_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private InMemoryUserAccountRepository accounts;
    private RecordingPasswordHasher passwordHasher;
    private RecordingIdentityEventPublisher events;
    private IdentityService service;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryUserAccountRepository();
        passwordHasher = new RecordingPasswordHasher();
        events = new RecordingIdentityEventPublisher();
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        service = new IdentityService(accounts, passwordHasher, events, clock, () -> FIXED_ID);
    }

    @Test
    void registerCanonicalizesUsernameHashesUntouchedPasswordAndPublishesEvent() {
        String rawPassword = "  secret!!";
        AuthenticatedUser user = service.register("  Alice  ", rawPassword);

        assertEquals(FIXED_ID, user.id());
        assertEquals("alice", user.username());
        assertFalse(user.toString().contains(rawPassword));

        UserAccount stored = accounts.findByUsername(Username.parse("alice")).orElseThrow();
        assertEquals(FIXED_ID, stored.id());
        assertEquals("alice", stored.username().value());
        assertEquals(FIXED_INSTANT, stored.createdAt());
        assertTrue(stored.passwordHash().startsWith("sha256:"));
        assertFalse(stored.passwordHash().contains(rawPassword));
        assertEquals(1, passwordHasher.hashedInputs().size());
        assertEquals(rawPassword, passwordHasher.hashedInputs().getFirst());

        assertEquals(1, events.events().size());
        UserRegisteredV1 event = events.events().getFirst();
        assertEquals(FIXED_ID, event.userId());
        assertEquals("alice", event.username());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertFalse(event.toString().contains(rawPassword));
        assertFalse(event.toString().contains(stored.passwordHash()));
    }

    @Test
    void registerRejectsDuplicateUsernameCaseInsensitively() {
        service.register("alice", "password1");
        RecordingIdentityEventPublisher secondEvents = new RecordingIdentityEventPublisher();
        IdentityService second = new IdentityService(
                accounts,
                passwordHasher,
                secondEvents,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                () -> UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        );

        assertThrows(DuplicateUsernameException.class, () -> second.register("ALICE", "password2"));
        assertTrue(secondEvents.events().isEmpty());
        assertEquals(1, events.events().size());
    }

    @Test
    void authenticateSucceedsWithCorrectPassword() {
        service.register("bob", "password1");
        AuthenticatedUser user = service.authenticate("Bob", "password1");
        assertEquals("bob", user.username());
        assertEquals(FIXED_ID, user.id());
    }

    @Test
    void unknownUsernameAndWrongPasswordShareInvalidCredentialsCategory() {
        service.register("carol", "password1");

        InvalidCredentialsException unknown = assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate("nobody", "password1")
        );
        InvalidCredentialsException wrong = assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate("carol", "password2")
        );

        assertInstanceOf(InvalidCredentialsException.class, unknown);
        assertInstanceOf(InvalidCredentialsException.class, wrong);
        assertEquals(unknown.getMessage(), wrong.getMessage());
        assertEquals(1, events.events().size());
    }

    @Test
    void failedAuthenticatePublishesNoEvent() {
        assertThrows(InvalidCredentialsException.class, () -> service.authenticate("ghost", "password1"));
        assertTrue(events.events().isEmpty());
    }

    @Test
    void rejectsInvalidUsernameAndPasswordBoundaries() {
        assertThrows(InvalidUsernameException.class, () -> service.register("ab", "password1"));
        assertThrows(InvalidPasswordException.class, () -> service.register("validuser", "short"));
        assertThrows(
                InvalidPasswordException.class,
                () -> service.register("validuser", "x".repeat(73))
        );
        service.register("validuser", "x".repeat(72));
        assertTrue(events.events().size() >= 1);

        assertThrows(InvalidUsernameException.class, () -> service.authenticate("!!", "password1"));
        assertThrows(InvalidPasswordException.class, () -> service.authenticate("validuser", "short"));
        assertTrue(events.events().stream().noneMatch(e -> e.username().equals("!!")));
    }

    @Test
    void passwordUtf8ByteLimitUsesBytesNotCodePoints() {
        String tooManyBytes = "€".repeat(25);
        assertTrue(tooManyBytes.codePointCount(0, tooManyBytes.length()) >= 8);
        assertTrue(tooManyBytes.getBytes(StandardCharsets.UTF_8).length > 72);
        assertThrows(InvalidPasswordException.class, () -> service.register("eurouser", tooManyBytes));
        assertTrue(events.events().isEmpty());
    }

    @Test
    void findByIdReturnsSafeAuthenticatedUser() {
        service.register("dave", "password1");
        assertEquals("dave", service.findById(FIXED_ID).orElseThrow().username());
        assertTrue(service.findById(UUID.fromString("00000000-0000-0000-0000-000000000099")).isEmpty());
    }
}
