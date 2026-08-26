--liquibase formatted sql

--changeset watchnest:006-cms-account
CREATE TABLE cms_account (
    id UUID NOT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cms_account PRIMARY KEY (id),
    CONSTRAINT uk_cms_account_username UNIQUE (username),
    CONSTRAINT ck_cms_account_username CHECK (username ~ '^[a-z0-9._-]{3,32}$'),
    CONSTRAINT ck_cms_account_password_hash CHECK (char_length(btrim(password_hash)) > 0)
);
