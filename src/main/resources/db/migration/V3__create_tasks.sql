CREATE TABLE tasks (
    id          UUID         NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(50)  NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE,
    project_id  UUID         NOT NULL,
    assignee_id UUID,
    reporter_id UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_tasks          PRIMARY KEY (id),
    CONSTRAINT fk_tasks_project  FOREIGN KEY (project_id)  REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users    (id),
    CONSTRAINT fk_tasks_reporter FOREIGN KEY (reporter_id) REFERENCES users    (id)
);

CREATE INDEX idx_tasks_project_id  ON tasks (project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_status      ON tasks (status);
