CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
   id BIGSERIAL PRIMARY KEY,

   name VARCHAR(50) NOT NULL,
   description VARCHAR(255),

   is_active BOOLEAN NOT NULL DEFAULT TRUE,

   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,

-- Identity
   uuid UUID NOT NULL DEFAULT gen_random_uuid(),
   email VARCHAR(255) NOT NULL,
   username VARCHAR(100),
   password_hash VARCHAR(255) NOT NULL,

-- Basic profile
   first_name VARCHAR(100) NOT NULL,
   middle_name VARCHAR(100),
   last_name VARCHAR(100) NOT NULL,
   display_name VARCHAR(255),
   phone_country_code VARCHAR(10),
   phone_number VARCHAR(20),

-- Status / lifecycle
   is_active BOOLEAN NOT NULL DEFAULT TRUE,
   is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
   is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
   is_account_locked BOOLEAN NOT NULL DEFAULT FALSE,
   is_password_change_required BOOLEAN NOT NULL DEFAULT FALSE,

-- Security / auth tracking
   failed_login_attempts INT NOT NULL DEFAULT 0,
   last_login_at TIMESTAMP,
   last_login_ip VARCHAR(45),
   password_changed_at TIMESTAMP,
   account_locked_at TIMESTAMP,

-- Multi-tenant / ownership
   tenant_id BIGINT,
   created_by BIGINT,
   updated_by BIGINT,

-- Audit
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP,

-- Constraints
   CONSTRAINT uq_users_uuid UNIQUE (uuid),
   CONSTRAINT uq_users_email UNIQUE (email),
   CONSTRAINT uq_users_username UNIQUE (username),

   CONSTRAINT fk_users_created_by
       FOREIGN KEY (created_by) REFERENCES users(id),

   CONSTRAINT fk_users_updated_by
       FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users(id),

    CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role_id)
);

-- Indexes
CREATE INDEX idx_roles_name ON roles(name);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Seed default roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System administrator'),
('DOCTOR', 'Medical practitioner'),
('RECEPTIONIST', 'Front desk and appointment manager'),
('PATIENT', 'Patient portal user');