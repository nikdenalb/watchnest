--liquibase formatted sql

--changeset watchnest:008-cms-account-demo
ALTER TABLE cms_account
    ADD COLUMN demo BOOLEAN NOT NULL DEFAULT FALSE;
