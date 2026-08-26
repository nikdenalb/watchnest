package dev.watchnest.plannerapp.cms.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CmsAccountJpaRepository extends JpaRepository<CmsAccountEntity, UUID> {

    Optional<CmsAccountEntity> findByUsername(String username);
}
