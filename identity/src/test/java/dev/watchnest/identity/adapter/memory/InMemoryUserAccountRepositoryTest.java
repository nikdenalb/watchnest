package dev.watchnest.identity.adapter.memory;

import dev.watchnest.identity.domain.DuplicateUsernameException;
import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUserAccountRepositoryTest {

    @Test
    void insertRejectsDuplicateUsernameAtomicallyUnderContention() throws Exception {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        Username username = Username.parse("alice");
        Instant createdAt = Instant.parse("2026-08-02T12:00:00Z");

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();

        try {
            @SuppressWarnings("unchecked")
            Future<?>[] futures = new Future[threads];
            for (int i = 0; i < threads; i++) {
                UUID id = UUID.fromString(String.format("10000000-0000-0000-0000-%012d", i + 1));
                UserAccount account = new UserAccount(id, username, "hash-" + i, createdAt);
                futures[i] = pool.submit(() -> {
                    start.await();
                    try {
                        repository.insert(account);
                        successes.incrementAndGet();
                    } catch (DuplicateUsernameException e) {
                        duplicates.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(threads - 1, duplicates.get());
        assertTrue(repository.findByUsername(username).isPresent());
    }

    @Test
    void sequentialDuplicateThrows() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        Username username = Username.parse("bob");
        Instant createdAt = Instant.parse("2026-08-02T12:00:00Z");
        UUID id = UUID.fromString("20000000-0000-0000-0000-000000000001");
        repository.insert(new UserAccount(id, username, "hash-a", createdAt));

        assertThrows(DuplicateUsernameException.class, () -> repository.insert(new UserAccount(
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                username,
                "hash-b",
                createdAt
        )));
    }

    @Test
    void findByIdReturnsPresentAndEmpty() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        UUID id = UUID.fromString("30000000-0000-0000-0000-000000000001");
        Username username = Username.parse("carol");
        Instant createdAt = Instant.parse("2026-08-02T12:00:00Z");
        repository.insert(new UserAccount(id, username, "hash", createdAt));

        assertTrue(repository.findById(id).isPresent());
        assertEquals("carol", repository.findById(id).orElseThrow().username().value());
        assertTrue(repository.findById(UUID.fromString("30000000-0000-0000-0000-000000000099")).isEmpty());
        assertTrue(repository.findByUsername(Username.parse("nobody")).isEmpty());
    }
}
