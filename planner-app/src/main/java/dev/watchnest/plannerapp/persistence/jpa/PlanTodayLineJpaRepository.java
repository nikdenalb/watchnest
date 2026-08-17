package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanTodayLineJpaRepository extends JpaRepository<PlanTodayLineEntity, UUID> {

    List<PlanTodayLineEntity> findByPlanTodayIdOrderBySortIndexAsc(UUID planTodayId);

    void deleteByPlanTodayId(UUID planTodayId);
}
