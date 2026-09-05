--liquibase formatted sql

--changeset watchnest:009-diary-only
DROP TABLE plan_today_line;
DROP TABLE plan_today;
DROP TABLE forward_plan_item;

ALTER TABLE library_profile
    DROP COLUMN weekday_episode_limit,
    DROP COLUMN weekend_episode_limit,
    DROP COLUMN treat_plan_as_watched;
