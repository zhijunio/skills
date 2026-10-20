CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    completed BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tasks_created_at ON tasks (created_at DESC);
