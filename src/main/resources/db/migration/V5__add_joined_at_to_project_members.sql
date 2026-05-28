ALTER TABLE project_members
    ADD COLUMN joined_at TIMESTAMPTZ NOT NULL DEFAULT now();
