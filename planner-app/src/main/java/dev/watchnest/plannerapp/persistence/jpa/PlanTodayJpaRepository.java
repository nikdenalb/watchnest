package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanTodayJpaRepository extends JpaRepository<PlanTodayEntity, UUID> {

    Optional<PlanTodayEntity> findByOwnerId(UUID ownerId);

    void deleteByOwnerId(UUID ownerId);
}
