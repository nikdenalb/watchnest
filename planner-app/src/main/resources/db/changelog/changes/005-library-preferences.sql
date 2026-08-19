--liquibase formatted sql

--changeset watchnest:005-library-preferences
ALTER TABLE library_profile
    ADD COLUMN treat_plan_as_watched BOOLEAN NOT NULL DEFAULT FALSE;
