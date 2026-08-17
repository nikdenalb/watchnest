package dev.watchnest.plannerapp.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import dev.watchnest.planner.domain.PlanLineSource;

import java.util.UUID;

@Entity
@Table(name = "plan_today_line")
public class PlanTodayLineEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "plan_today_id", nullable = false)
    private UUID planTodayId;

    @Column(name = "content_title", nullable = false, length = 120)
    private String contentTitle;

    @Column(name = "checked", nullable = false)
    private boolean checked;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private PlanLineSource source;

    @Column(name = "sort_index", nullable = false)
    private int sortIndex;

    protected PlanTodayLineEntity() {
    }

    public PlanTodayLineEntity(
            UUID id,
            UUID planTodayId,
            String contentTitle,
            boolean checked,
            PlanLineSource source,
            int sortIndex
    ) {
        this.id = id;
        this.planTodayId = planTodayId;
        this.contentTitle = contentTitle;
        this.checked = checked;
        this.source = source;
        this.sortIndex = sortIndex;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanTodayId() {
        return planTodayId;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public boolean isChecked() {
        return checked;
    }

    public PlanLineSource getSource() {
        return source;
    }

    public int getSortIndex() {
        return sortIndex;
    }
}
