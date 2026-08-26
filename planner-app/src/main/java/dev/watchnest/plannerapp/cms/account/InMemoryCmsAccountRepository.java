package dev.watchnest.plannerapp.cms.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("memory")
public class InMemoryCmsAccountRepository implements CmsAccountRepository {

    private final ConcurrentHashMap<String, CmsAccount> byUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<CmsAccount> findByUsername(String username) {
        Objects.requireNonNull(username, "username");
        return Optional.ofNullable(byUsername.get(username));
    }

    public void seed(CmsAccount account) {
        Objects.requireNonNull(account, "account");
        byUsername.put(account.username(), account);
    }
}
