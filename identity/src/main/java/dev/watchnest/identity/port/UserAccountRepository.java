package dev.watchnest.identity.port;

import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {

    void insert(UserAccount account);

    Optional<UserAccount> findByUsername(Username username);

    Optional<UserAccount> findById(UUID id);
}
