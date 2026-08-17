package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForwardPlanItemJpaRepository extends JpaRepository<ForwardPlanItemEntity, UUID> {

    List<ForwardPlanItemEntity> findByOwnerIdAndPlannedForBetweenOrderByPlannedForAscSortIndexAsc(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    );

    List<ForwardPlanItemEntity> findByOwnerIdAndPlannedForLessThanOrderByPlannedForAscSortIndexAsc(
            UUID ownerId,
            LocalDate plannedFor
    );

    List<ForwardPlanItemEntity> findByOwnerIdAndPlannedForOrderBySortIndexAsc(UUID ownerId, LocalDate plannedFor);

    Optional<ForwardPlanItemEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    int countByOwnerIdAndPlannedFor(UUID ownerId, LocalDate plannedFor);

    @Query("""
            select coalesce(max(item.sortIndex), -1)
            from ForwardPlanItemEntity item
            where item.ownerId = :ownerId
            """)
    Integer maxSortIndexByOwnerId(@Param("ownerId") UUID ownerId);
}
