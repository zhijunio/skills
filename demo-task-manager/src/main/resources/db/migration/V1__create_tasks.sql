CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT tasks_status_check CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX tasks_created_at_idx ON tasks (created_at DESC);
