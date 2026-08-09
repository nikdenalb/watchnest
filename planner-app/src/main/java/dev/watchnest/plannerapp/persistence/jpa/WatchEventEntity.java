package dev.watchnest.plannerapp.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "watch_event")
public class WatchEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "watched_on", nullable = false)
    private LocalDate watchedOn;

    @Column(name = "content_title", nullable = false, length = 120)
    private String contentTitle;

    protected WatchEventEntity() {
    }

    public WatchEventEntity(UUID id, UUID ownerId, LocalDate watchedOn, String contentTitle) {
        this.id = id;
        this.ownerId = ownerId;
        this.watchedOn = watchedOn;
        this.contentTitle = contentTitle;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDate getWatchedOn() {
        return watchedOn;
    }

    public String getContentTitle() {
        return contentTitle;
    }
}
