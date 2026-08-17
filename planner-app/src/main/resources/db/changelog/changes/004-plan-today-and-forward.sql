--liquibase formatted sql

--changeset watchnest:004-plan-today-and-forward
CREATE TABLE forward_plan_item (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    planned_for DATE NOT NULL,
    content_title VARCHAR(120) NOT NULL,
    sort_index INT NOT NULL,
    CONSTRAINT pk_forward_plan_item PRIMARY KEY (id),
    CONSTRAINT fk_forward_plan_item_library_profile
        FOREIGN KEY (owner_id) REFERENCES library_profile (id) ON DELETE CASCADE
);

CREATE INDEX forward_plan_item_owner_planned_sort
    ON forward_plan_item (owner_id, planned_for, sort_index);

CREATE TABLE plan_today (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    for_date DATE NOT NULL,
    CONSTRAINT pk_plan_today PRIMARY KEY (id),
    CONSTRAINT uk_plan_today_owner UNIQUE (owner_id),
    CONSTRAINT fk_plan_today_library_profile
        FOREIGN KEY (owner_id) REFERENCES library_profile (id) ON DELETE CASCADE
);

CREATE TABLE plan_today_line (
    id UUID NOT NULL,
    plan_today_id UUID NOT NULL,
    content_title VARCHAR(120) NOT NULL,
    checked BOOLEAN NOT NULL,
    source VARCHAR(16) NOT NULL,
    sort_index INT NOT NULL,
    CONSTRAINT pk_plan_today_line PRIMARY KEY (id),
    CONSTRAINT fk_plan_today_line_plan_today
        FOREIGN KEY (plan_today_id) REFERENCES plan_today (id) ON DELETE CASCADE
);

CREATE INDEX plan_today_line_plan_sort ON plan_today_line (plan_today_id, sort_index);
