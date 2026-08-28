package dev.watchnest.plannerapp.cms.auth;

import java.util.Optional;
import java.util.UUID;

public interface CmsSessionStore {

    String create(UUID accountId, String username, boolean demo);

    Optional<CmsSession> authenticate(String rawToken);

    void revoke(String rawToken);
}
