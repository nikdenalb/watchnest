package dev.watchnest.plannerapp.cms.account;

import java.util.Optional;

public interface CmsAccountRepository {

    Optional<CmsAccount> findByUsername(String username);
}
