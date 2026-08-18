package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchEventJpaRepository extends JpaRepository<WatchEventEntity, UUID> {

    Optional<WatchEventEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    long countByOwnerIdAndWatchedOn(UUID ownerId, LocalDate watchedOn);

    List<WatchEventEntity> findByOwnerIdAndWatchedOnBetweenOrderByWatchedOnDescContentTitleAsc(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    );
}
