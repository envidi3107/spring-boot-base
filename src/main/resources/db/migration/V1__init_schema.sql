-- Schema ban đầu cho module auth và user.
-- Mọi bảng kế thừa BaseEntity đều có: id (UUID v7 do ứng dụng sinh), version, created_*/updated_*.
-- Role và tài khoản admin được tạo bởi DataInitializer lúc khởi động, không seed ở đây.

CREATE TABLE roles (
    id          UUID         NOT NULL,
    version     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  VARCHAR(100),
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id          UUID         NOT NULL,
    version     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  VARCHAR(100),
    username    VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    full_name   VARCHAR(255),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE refresh_tokens (
    id          UUID         NOT NULL,
    version     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  VARCHAR(100),
    token_hash  VARCHAR(64)  NOT NULL,
    user_id     UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Thu hồi toàn bộ phiên theo user và job dọn token hết hạn
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
