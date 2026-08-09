--liquibase formatted sql

--changeset watchnest:002-library
CREATE TABLE library_profile (
    id UUID NOT NULL,
    display_name VARCHAR(32) NOT NULL,
    weekday_episode_limit INT NOT NULL DEFAULT 2,
    weekend_episode_limit INT NOT NULL DEFAULT 4,
    CONSTRAINT pk_library_profile PRIMARY KEY (id),
    CONSTRAINT fk_library_profile_user_account
        FOREIGN KEY (id) REFERENCES user_account (id) ON DELETE CASCADE
);

CREATE TABLE watch_event (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    watched_on DATE NOT NULL,
    content_title VARCHAR(120) NOT NULL,
    CONSTRAINT pk_watch_event PRIMARY KEY (id),
    CONSTRAINT fk_watch_event_library_profile
        FOREIGN KEY (owner_id) REFERENCES library_profile (id) ON DELETE CASCADE
);

CREATE INDEX watch_event_owner_id ON watch_event (owner_id);
