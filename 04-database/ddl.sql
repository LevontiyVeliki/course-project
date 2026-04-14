-- Drop tables if they exist (for clean re-run)
DROP TABLE IF EXISTS reminders CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS task_lists CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Table: users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

COMMENT ON TABLE users IS 'Registered application users';
COMMENT ON COLUMN users.id IS 'Unique identifier';
COMMENT ON COLUMN users.username IS 'Login (unique)';
COMMENT ON COLUMN users.email IS 'Email address for login and contact';
COMMENT ON COLUMN users.password_hash IS 'BCrypt password hash';
COMMENT ON COLUMN users.full_name IS 'Full name (optional)';
COMMENT ON COLUMN users.role IS 'Role: USER or ADMIN';
COMMENT ON COLUMN users.created_at IS 'Registration timestamp';
COMMENT ON COLUMN users.updated_at IS 'Last profile update timestamp';

-- Table: task_lists
CREATE TABLE task_lists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_task_lists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_lists_status CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

COMMENT ON TABLE task_lists IS 'Task lists bound to a specific date';
COMMENT ON COLUMN task_lists.user_id IS 'Owner of the list';
COMMENT ON COLUMN task_lists.name IS 'List name';
COMMENT ON COLUMN task_lists.target_date IS 'Execution date';
COMMENT ON COLUMN task_lists.status IS 'Status: ACTIVE or COMPLETED';

-- Index for fast lookup of user lists by date
CREATE INDEX idx_task_lists_user_date ON task_lists(user_id, target_date);

-- Table: tasks
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    task_list_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_tasks_list FOREIGN KEY (task_list_id) REFERENCES task_lists(id) ON DELETE CASCADE,
    CONSTRAINT chk_tasks_status CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

COMMENT ON TABLE tasks IS 'Individual tasks within a list';
COMMENT ON COLUMN tasks.task_list_id IS 'Parent task list';
COMMENT ON COLUMN tasks.description IS 'Task text';
COMMENT ON COLUMN tasks.status IS 'Status: PENDING, COMPLETED, OVERDUE';
COMMENT ON COLUMN tasks.priority IS 'Priority: LOW, MEDIUM, HIGH';
COMMENT ON COLUMN tasks.order_index IS 'Sort order in UI';

-- Index for sorting tasks within a list
CREATE INDEX idx_tasks_list_order ON tasks(task_list_id, order_index);

-- Table: reminders
CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    task_list_id BIGINT,
    task_id BIGINT,
    trigger_time TIMESTAMP NOT NULL,
    message TEXT,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reminders_list FOREIGN KEY (task_list_id) REFERENCES task_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT chk_reminders_target CHECK (
        (task_list_id IS NOT NULL AND task_id IS NULL) OR
        (task_list_id IS NULL AND task_id IS NOT NULL)
    )
);

COMMENT ON TABLE reminders IS 'Reminders for task lists or individual tasks';
COMMENT ON COLUMN reminders.trigger_time IS 'Time when reminder should fire';
COMMENT ON COLUMN reminders.message IS 'Custom message (if NULL, auto-generated)';
COMMENT ON COLUMN reminders.is_sent IS 'Flag indicating if notification was sent';

-- Index for finding unsent reminders
CREATE INDEX idx_reminders_unsent ON reminders(is_sent, trigger_time) WHERE is_sent = FALSE;

-- Index for fast user lookup by email (for authentication)
CREATE INDEX idx_users_email ON users(email);