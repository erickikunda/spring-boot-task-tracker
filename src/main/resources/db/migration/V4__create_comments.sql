CREATE TABLE comments (
    id         UUID        NOT NULL,
    body       TEXT        NOT NULL,
    task_id    UUID        NOT NULL,
    author_id  UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_comments        PRIMARY KEY (id),
    CONSTRAINT fk_comments_task   FOREIGN KEY (task_id)   REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_comments_task_id ON comments (task_id);
