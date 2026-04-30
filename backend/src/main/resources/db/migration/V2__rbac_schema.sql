-- V2__rbac_schema.sql: Introduce RBAC tables with nullable tenant_id

-- -------------------------------------------------
-- 1. PERMISSIONS (global catalog, no tenant_id)
-- -------------------------------------------------
CREATE TABLE permissions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(100) NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    module        VARCHAR(100) NOT NULL,
    action        VARCHAR(50) NOT NULL,
    description   VARCHAR(255),
    is_system     BOOLEAN NOT NULL DEFAULT TRUE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    BIGINT,
    updated_by    BIGINT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP
);
CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_module ON permissions(module);
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE INDEX idx_permissions_deleted_at ON permissions(deleted_at);

-- -------------------------------------------------
-- 2. ROLES (now with nullable tenant_id)
-- -------------------------------------------------
ALTER TABLE roles ADD COLUMN tenant_id BIGINT;
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);

-- -------------------------------------------------
-- 3. ROLE -> PERMISSIONS (join table with optional tenant_id)
-- -------------------------------------------------
CREATE TABLE role_permissions (
    id            BIGSERIAL PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    tenant_id     BIGINT,
    assigned_by   BIGINT,
    assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);
CREATE INDEX idx_role_perm_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_perm_perm_id ON role_permissions(permission_id);
CREATE INDEX idx_role_perm_tenant_id ON role_permissions(tenant_id);

-- -------------------------------------------------
-- 4. USER -> ROLES (explicit join entity, replaces existing ManyToMany)
-- -------------------------------------------------
CREATE TABLE user_role_assignments (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    tenant_id     BIGINT,
    assigned_by   BIGINT,
    assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_from    TIMESTAMP,
    valid_until   TIMESTAMP,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_role_assignments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_role_assignments_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT uq_user_role_assignments_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX idx_user_role_assignments_user_id ON user_role_assignments(user_id);
CREATE INDEX idx_user_role_assignments_role_id ON user_role_assignments(role_id);
CREATE INDEX idx_user_role_assignments_tenant_id ON user_role_assignments(tenant_id);
CREATE INDEX idx_user_role_assignments_is_active ON user_role_assignments(is_active);

-- -------------------------------------------------
-- 5. USER PERMISSION OVERRIDES (ALLOW / DENY) with nullable tenant_id
-- -------------------------------------------------
CREATE TABLE user_permission_overrides (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    effect        VARCHAR(10) NOT NULL CHECK (effect IN ('ALLOW','DENY')),
    tenant_id     BIGINT,
    reason        VARCHAR(255),
    assigned_by   BIGINT,
    assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_from    TIMESTAMP,
    valid_until   TIMESTAMP,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_upo_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_upo_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT uq_upo_user_perm_effect UNIQUE (user_id, permission_id, effect)
);
CREATE INDEX idx_upo_user_id ON user_permission_overrides(user_id);
CREATE INDEX idx_upo_perm_id ON user_permission_overrides(permission_id);
CREATE INDEX idx_upo_effect ON user_permission_overrides(effect);
CREATE INDEX idx_upo_tenant_id ON user_permission_overrides(tenant_id);
CREATE INDEX idx_upo_is_active ON user_permission_overrides(is_active);
