package dev.watchnest.plannerapp.cms.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCmsSessionStore implements CmsSessionStore {

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CmsSession> sessions = new ConcurrentHashMap<>();

    public InMemoryCmsSessionStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String create(UUID accountId, String username) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(username, "username");
        String token = newToken();
        Instant now = Instant.now(clock);
        synchronized (sessions) {
            removeExpired(now);
            sessions.put(token, new CmsSession(accountId, username, now));
        }
        return token;
    }

    @Override
    public Optional<CmsSession> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        synchronized (sessions) {
            removeExpired(now);
            CmsSession session = sessions.get(rawToken);
            if (session == null) {
                return Optional.empty();
            }
            CmsSession touched = new CmsSession(session.accountId(), session.username(), now);
            sessions.put(rawToken, touched);
            return Optional.of(touched);
        }
    }

    @Override
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        synchronized (sessions) {
            sessions.remove(rawToken);
        }
    }

    int size() {
        return sessions.size();
    }

    private void removeExpired(Instant now) {
        Iterator<Map.Entry<String, CmsSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CmsSession> entry = iterator.next();
            if (isExpired(entry.getValue().lastAccessedAt(), now)) {
                iterator.remove();
            }
        }
    }

    private static boolean isExpired(Instant lastAccessedAt, Instant now) {
        return !now.isBefore(lastAccessedAt.plus(IDLE_TIMEOUT));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
