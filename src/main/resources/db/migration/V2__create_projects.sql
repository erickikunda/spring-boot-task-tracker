CREATE TABLE projects (
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    owner_id    UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_projects       PRIMARY KEY (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_projects_owner_id ON projects (owner_id);

CREATE TABLE project_members (
    project_id UUID NOT NULL,
    user_id    UUID NOT NULL,

    CONSTRAINT pk_project_members PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE
);
