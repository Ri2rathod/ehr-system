-- V18: Create treatment_plans and treatment_plan_items tables (child clinical domain of encounters)
-- Plus treatment plan/item permissions and role assignments
-- Clinical intention is documented here; medication orders belong to the future Prescription module.

CREATE TABLE treatment_plans (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    encounter_id BIGINT NOT NULL,

    title VARCHAR(500) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    goals TEXT,
    instructions TEXT,
    follow_up_instructions TEXT,
    notes TEXT,

    start_date DATE,
    end_date DATE,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_treatment_plan_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounters(id),

    CONSTRAINT ck_treatment_plan_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Required Indexes
CREATE INDEX idx_treatment_plan_uuid ON treatment_plans(uuid);
-- Most common query: Encounter -> treatment plans (by encounter, excluding deleted)
CREATE INDEX idx_treatment_plan_encounter ON treatment_plans(encounter_id, deleted_at);
CREATE INDEX idx_treatment_plan_status ON treatment_plans(status);
CREATE INDEX idx_treatment_plan_deleted_at ON treatment_plans(deleted_at);
-- Optimize: Encounter -> active treatment plan
CREATE INDEX idx_treatment_plan_encounter_status ON treatment_plans(encounter_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE treatment_plan_items (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    treatment_plan_id BIGINT NOT NULL,

    diagnosis_id BIGINT,

    treatment_type VARCHAR(30) NOT NULL,

    name VARCHAR(500) NOT NULL,
    description TEXT,
    instructions TEXT,

    frequency VARCHAR(200),
    duration INT,
    duration_unit VARCHAR(20),

    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',

    start_date DATE,
    end_date DATE,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_treatment_item_plan
        FOREIGN KEY (treatment_plan_id)
        REFERENCES treatment_plans(id),

    CONSTRAINT fk_treatment_item_diagnosis
        FOREIGN KEY (diagnosis_id)
        REFERENCES diagnoses(id),

    CONSTRAINT ck_treatment_item_type
        CHECK (treatment_type IN ('LIFESTYLE', 'DIET', 'EXERCISE', 'PHYSIOTHERAPY',
                                  'BEHAVIORAL', 'EDUCATION', 'MONITORING', 'FOLLOW_UP',
                                  'PROCEDURE', 'OTHER')),

    CONSTRAINT ck_treatment_item_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT ck_treatment_item_status
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),

    CONSTRAINT ck_treatment_item_duration_unit
        CHECK (duration_unit IS NULL OR duration_unit IN ('DAYS', 'WEEKS', 'MONTHS'))
);

-- Required Indexes
CREATE INDEX idx_treatment_item_uuid ON treatment_plan_items(uuid);
CREATE INDEX idx_treatment_item_plan ON treatment_plan_items(treatment_plan_id, deleted_at);
CREATE INDEX idx_treatment_item_diagnosis ON treatment_plan_items(diagnosis_id);
CREATE INDEX idx_treatment_item_status ON treatment_plan_items(status);
CREATE INDEX idx_treatment_item_deleted_at ON treatment_plan_items(deleted_at);

-- =============================================================
-- Permissions
-- =============================================================
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('TREATMENT_PLAN_VIEW',      'View Treatment Plans',      'TREATMENT_PLAN', 'READ',   'View treatment plans recorded for an encounter'),
    ('TREATMENT_PLAN_CREATE',    'Create Treatment Plan',     'TREATMENT_PLAN', 'CREATE', 'Create a treatment plan for an encounter'),
    ('TREATMENT_PLAN_UPDATE',    'Update Treatment Plan',     'TREATMENT_PLAN', 'UPDATE', 'Edit a treatment plan'),
    ('TREATMENT_PLAN_ACTIVATE',  'Activate Treatment Plan',   'TREATMENT_PLAN', 'UPDATE', 'Activate a draft treatment plan'),
    ('TREATMENT_PLAN_COMPLETE',  'Complete Treatment Plan',   'TREATMENT_PLAN', 'UPDATE', 'Complete an active treatment plan'),
    ('TREATMENT_PLAN_CANCEL',    'Cancel Treatment Plan',     'TREATMENT_PLAN', 'DELETE', 'Cancel a treatment plan'),
    ('TREATMENT_PLAN_DELETE',    'Delete Treatment Plan',     'TREATMENT_PLAN', 'DELETE', 'Remove a treatment plan from the encounter'),
    ('TREATMENT_ITEM_VIEW',      'View Treatment Items',      'TREATMENT_ITEM', 'READ',   'View treatment items of a treatment plan'),
    ('TREATMENT_ITEM_CREATE',    'Create Treatment Item',     'TREATMENT_ITEM', 'CREATE', 'Add a treatment item to a treatment plan'),
    ('TREATMENT_ITEM_UPDATE',    'Update Treatment Item',     'TREATMENT_ITEM', 'UPDATE', 'Edit a treatment item'),
    ('TREATMENT_ITEM_START',     'Start Treatment Item',      'TREATMENT_ITEM', 'UPDATE', 'Progress a planned treatment item to in progress'),
    ('TREATMENT_ITEM_COMPLETE',  'Complete Treatment Item',   'TREATMENT_ITEM', 'UPDATE', 'Complete an in-progress treatment item'),
    ('TREATMENT_ITEM_CANCEL',    'Cancel Treatment Item',     'TREATMENT_ITEM', 'DELETE', 'Cancel a treatment item'),
    ('TREATMENT_ITEM_DELETE',    'Delete Treatment Item',     'TREATMENT_ITEM', 'DELETE', 'Remove a treatment item from the plan')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant all treatment permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('TREATMENT_PLAN_VIEW', 'TREATMENT_PLAN_CREATE', 'TREATMENT_PLAN_UPDATE',
                 'TREATMENT_PLAN_ACTIVATE', 'TREATMENT_PLAN_COMPLETE', 'TREATMENT_PLAN_CANCEL',
                 'TREATMENT_PLAN_DELETE',
                 'TREATMENT_ITEM_VIEW', 'TREATMENT_ITEM_CREATE', 'TREATMENT_ITEM_UPDATE',
                 'TREATMENT_ITEM_START', 'TREATMENT_ITEM_COMPLETE', 'TREATMENT_ITEM_CANCEL',
                 'TREATMENT_ITEM_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: full clinical access to treatment plans and items
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('TREATMENT_PLAN_VIEW', 'TREATMENT_PLAN_CREATE', 'TREATMENT_PLAN_UPDATE',
                 'TREATMENT_PLAN_ACTIVATE', 'TREATMENT_PLAN_COMPLETE', 'TREATMENT_PLAN_CANCEL',
                 'TREATMENT_PLAN_DELETE',
                 'TREATMENT_ITEM_VIEW', 'TREATMENT_ITEM_CREATE', 'TREATMENT_ITEM_UPDATE',
                 'TREATMENT_ITEM_START', 'TREATMENT_ITEM_COMPLETE', 'TREATMENT_ITEM_CANCEL',
                 'TREATMENT_ITEM_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: view treatment plans (front desk), no clinical documentation rights
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('TREATMENT_PLAN_VIEW', 'TREATMENT_ITEM_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PATIENT: view own treatment plans
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN ('TREATMENT_PLAN_VIEW', 'TREATMENT_ITEM_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
