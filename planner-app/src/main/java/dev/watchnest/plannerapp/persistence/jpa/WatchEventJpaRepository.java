package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WatchEventJpaRepository extends JpaRepository<WatchEventEntity, UUID> {

    List<WatchEventEntity> findByOwnerId(UUID ownerId);
}
