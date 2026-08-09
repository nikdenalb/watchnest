--liquibase formatted sql

--changeset watchnest:001-user-accounts
CREATE TABLE user_account (
    id UUID NOT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_account PRIMARY KEY (id),
    CONSTRAINT uk_user_account_username UNIQUE (username)
);
