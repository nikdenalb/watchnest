--liquibase formatted sql

--changeset watchnest:007-catalog-title
CREATE TABLE catalog_title (
    id UUID NOT NULL,
    title_type VARCHAR(32) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_en_key VARCHAR(255) NOT NULL,
    name_original VARCHAR(255) NOT NULL,
    release_year INT NOT NULL,
    description TEXT NULL,
    genres VARCHAR(1000) NULL,
    countries VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_catalog_title PRIMARY KEY (id),
    CONSTRAINT uk_catalog_title_natural_key UNIQUE (name_en_key, release_year, title_type),
    CONSTRAINT ck_catalog_title_type CHECK (
        title_type IN ('FILM', 'TV_SERIES', 'MINI_SERIES', 'TV_SHOW')
    ),
    CONSTRAINT ck_catalog_title_year CHECK (release_year >= 1000 AND release_year <= 9999),
    CONSTRAINT ck_catalog_title_name_en CHECK (char_length(btrim(name_en)) > 0),
    CONSTRAINT ck_catalog_title_name_original CHECK (char_length(btrim(name_original)) > 0),
    CONSTRAINT ck_catalog_title_name_en_key CHECK (
        char_length(btrim(name_en_key)) > 0 AND name_en_key = lower(name_en_key)
    )
);
