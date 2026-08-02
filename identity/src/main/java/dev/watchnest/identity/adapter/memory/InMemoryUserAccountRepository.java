package dev.watchnest.identity.adapter.memory;

import dev.watchnest.identity.domain.DuplicateUsernameException;
import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;
import dev.watchnest.identity.port.UserAccountRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserAccountRepository implements UserAccountRepository {

    private final ConcurrentHashMap<String, UserAccount> byUsername = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UserAccount> byId = new ConcurrentHashMap<>();

    @Override
    public void insert(UserAccount account) {
        String key = account.username().value();
        UserAccount previous = byUsername.putIfAbsent(key, account);
        if (previous != null) {
            throw new DuplicateUsernameException(key);
        }
        byId.put(account.id(), account);
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        return Optional.ofNullable(byUsername.get(username.value()));
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }
}
