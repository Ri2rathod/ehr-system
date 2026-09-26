-- V17: Create diagnoses table (child clinical domain of encounters)
-- Plus diagnosis permissions and role assignments
-- Terminology-ready: code + code_system stored separately (ICD-10-CM, ICD-10, SNOMED CT, ...)

CREATE TABLE diagnoses (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    encounter_id BIGINT NOT NULL,

    code VARCHAR(32),
    code_system VARCHAR(50),
    name VARCHAR(500) NOT NULL,

    diagnosis_type VARCHAR(20) NOT NULL DEFAULT 'SECONDARY',
    clinical_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    onset_date DATE,
    resolved_date DATE,

    notes TEXT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_diagnosis_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounters(id),

    CONSTRAINT ck_diagnosis_type
        CHECK (diagnosis_type IN ('PRIMARY', 'SECONDARY')),

    CONSTRAINT ck_diagnosis_clinical_status
        CHECK (clinical_status IN ('ACTIVE', 'RESOLVED', 'INACTIVE'))
);

-- Required Indexes
CREATE INDEX idx_diagnosis_uuid ON diagnoses(uuid);
-- Most important query pattern: diagnoses are loaded from the encounter workspace
CREATE INDEX idx_diagnosis_encounter ON diagnoses(encounter_id, deleted_at);
CREATE INDEX idx_diagnosis_code ON diagnoses(code);
CREATE INDEX idx_diagnosis_code_system ON diagnoses(code_system);
CREATE INDEX idx_diagnosis_type ON diagnoses(diagnosis_type);
CREATE INDEX idx_diagnosis_clinical_status ON diagnoses(clinical_status);
CREATE INDEX idx_diagnosis_deleted_at ON diagnoses(deleted_at);

-- An encounter may have at most one active PRIMARY diagnosis
-- (soft-deleted diagnoses do not occupy the primary slot)
CREATE UNIQUE INDEX uq_diagnosis_primary_active
    ON diagnoses(encounter_id)
    WHERE diagnosis_type = 'PRIMARY' AND deleted_at IS NULL;

-- =============================================================
-- Permissions
-- =============================================================
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('DIAGNOSIS_VIEW',       'View Diagnoses',       'DIAGNOSIS', 'READ',   'View diagnoses recorded for an encounter'),
    ('DIAGNOSIS_CREATE',     'Create Diagnosis',     'DIAGNOSIS', 'CREATE', 'Record a diagnosis for an encounter'),
    ('DIAGNOSIS_UPDATE',     'Update Diagnosis',     'DIAGNOSIS', 'UPDATE', 'Edit a diagnosis recorded for an encounter'),
    ('DIAGNOSIS_DEACTIVATE', 'Deactivate Diagnosis', 'DIAGNOSIS', 'DELETE', 'Deactivate a diagnosis recorded for an encounter')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant all diagnosis permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('DIAGNOSIS_VIEW', 'DIAGNOSIS_CREATE', 'DIAGNOSIS_UPDATE', 'DIAGNOSIS_DEACTIVATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: full clinical access to diagnoses
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('DIAGNOSIS_VIEW', 'DIAGNOSIS_CREATE', 'DIAGNOSIS_UPDATE', 'DIAGNOSIS_DEACTIVATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: view diagnoses (front desk), no clinical documentation rights
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('DIAGNOSIS_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PATIENT: view own diagnoses (via own encounters)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN ('DIAGNOSIS_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
