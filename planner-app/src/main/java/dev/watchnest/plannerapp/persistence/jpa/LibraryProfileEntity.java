package dev.watchnest.plannerapp.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "library_profile")
public class LibraryProfileEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 32)
    private String displayName;

    @Column(name = "weekday_episode_limit", nullable = false)
    private int weekdayEpisodeLimit;

    @Column(name = "weekend_episode_limit", nullable = false)
    private int weekendEpisodeLimit;

    protected LibraryProfileEntity() {
    }

    public LibraryProfileEntity(
            UUID id,
            String displayName,
            int weekdayEpisodeLimit,
            int weekendEpisodeLimit
    ) {
        this.id = id;
        this.displayName = displayName;
        this.weekdayEpisodeLimit = weekdayEpisodeLimit;
        this.weekendEpisodeLimit = weekendEpisodeLimit;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWeekdayEpisodeLimit() {
        return weekdayEpisodeLimit;
    }

    public int getWeekendEpisodeLimit() {
        return weekendEpisodeLimit;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setWeekdayEpisodeLimit(int weekdayEpisodeLimit) {
        this.weekdayEpisodeLimit = weekdayEpisodeLimit;
    }

    public void setWeekendEpisodeLimit(int weekendEpisodeLimit) {
        this.weekendEpisodeLimit = weekendEpisodeLimit;
    }
}
