package dev.watchnest.plannerapp.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "forward_plan_item")
public class ForwardPlanItemEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "planned_for", nullable = false)
    private LocalDate plannedFor;

    @Column(name = "content_title", nullable = false, length = 120)
    private String contentTitle;

    @Column(name = "sort_index", nullable = false)
    private int sortIndex;

    protected ForwardPlanItemEntity() {
    }

    public ForwardPlanItemEntity(
            UUID id,
            UUID ownerId,
            LocalDate plannedFor,
            String contentTitle,
            int sortIndex
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.plannedFor = plannedFor;
        this.contentTitle = contentTitle;
        this.sortIndex = sortIndex;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDate getPlannedFor() {
        return plannedFor;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public int getSortIndex() {
        return sortIndex;
    }
}
