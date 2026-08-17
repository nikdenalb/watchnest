package dev.watchnest.plannerapp.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "plan_today")
public class PlanTodayEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;

    @Column(name = "for_date", nullable = false)
    private LocalDate forDate;

    protected PlanTodayEntity() {
    }

    public PlanTodayEntity(UUID id, UUID ownerId, LocalDate forDate) {
        this.id = id;
        this.ownerId = ownerId;
        this.forDate = forDate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDate getForDate() {
        return forDate;
    }

    public void setForDate(LocalDate forDate) {
        this.forDate = forDate;
    }
}
