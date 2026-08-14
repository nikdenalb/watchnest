--liquibase formatted sql

--changeset watchnest:003-watch-event-owner-date-index
CREATE INDEX watch_event_owner_id_watched_on ON watch_event (owner_id, watched_on);
