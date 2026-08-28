package dev.watchnest.plannerapp.cms.persistence;

import dev.watchnest.plannerapp.cms.account.CmsAccount;
import dev.watchnest.plannerapp.cms.account.CmsAccountRepository;
import dev.watchnest.plannerapp.cms.persistence.jpa.CmsAccountEntity;
import dev.watchnest.plannerapp.cms.persistence.jpa.CmsAccountJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@Profile("persistent")
public class JpaCmsAccountRepository implements CmsAccountRepository {

    private final CmsAccountJpaRepository jpa;

    public JpaCmsAccountRepository(CmsAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CmsAccount> findByUsername(String username) {
        Objects.requireNonNull(username, "username");
        return jpa.findByUsername(username).map(this::toDomain);
    }

    private CmsAccount toDomain(CmsAccountEntity entity) {
        return new CmsAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.isDemo(),
                entity.getCreatedAt()
        );
    }
}
