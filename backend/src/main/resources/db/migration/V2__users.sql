CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT users_email_lowercase_check CHECK (email = lower(email))
);

CREATE UNIQUE INDEX ux_users_email ON users (email);
CREATE INDEX ix_users_role ON users (role);
