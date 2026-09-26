-- V16: Create encounters, encounter_vitals, and encounter_sequences tables
-- Plus encounter/vitals permissions and role assignments

-- Create sequence table for concurrent-safe encounter number generation
CREATE TABLE encounter_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_encounter_sequences_year ON encounter_sequences(sequence_year);

-- Create encounters table
CREATE TABLE encounters (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    encounter_number VARCHAR(50) NOT NULL UNIQUE,

    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,

    encounter_type VARCHAR(50) NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    started_at TIMESTAMP,
    ended_at TIMESTAMP,

    chief_complaint TEXT,
    history_of_present_illness TEXT,
    clinical_notes TEXT,
    assessment TEXT,
    treatment_plan TEXT,
    follow_up_notes TEXT,

    cancellation_reason TEXT,

    tenant_id BIGINT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_encounter_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_encounter_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id),

    CONSTRAINT fk_encounter_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id)
);

-- Required Indexes
CREATE INDEX idx_encounter_uuid ON encounters(uuid);
CREATE INDEX idx_encounter_number ON encounters(encounter_number);
CREATE INDEX idx_encounter_patient ON encounters(patient_id);
CREATE INDEX idx_encounter_doctor ON encounters(doctor_id);
CREATE INDEX idx_encounter_appointment ON encounters(appointment_id);
CREATE INDEX idx_encounter_status ON encounters(status);
CREATE INDEX idx_encounter_type ON encounters(encounter_type);
CREATE INDEX idx_encounter_created_at ON encounters(created_at);
CREATE INDEX idx_encounter_deleted_at ON encounters(deleted_at);

-- An appointment may have at most one active (not deleted, not cancelled) encounter
CREATE UNIQUE INDEX uq_encounter_appointment_active
    ON encounters(appointment_id)
    WHERE deleted_at IS NULL AND status <> 'CANCELLED';

-- Create encounter_vitals table (child domain of encounters)
CREATE TABLE encounter_vitals (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    encounter_id BIGINT NOT NULL,

    temperature NUMERIC(5,2),
    heart_rate INT,
    respiratory_rate INT,
    systolic_bp INT,
    diastolic_bp INT,
    oxygen_saturation NUMERIC(5,2),
    weight NUMERIC(6,2),
    height NUMERIC(5,2),
    bmi NUMERIC(5,2),

    recorded_at TIMESTAMP NOT NULL,
    recorded_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_encounter_vitals_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounters(id)
);

-- Required Indexes
CREATE INDEX idx_encounter_vitals_uuid ON encounter_vitals(uuid);
CREATE INDEX idx_encounter_vitals_encounter ON encounter_vitals(encounter_id);
CREATE INDEX idx_encounter_vitals_recorded_at ON encounter_vitals(recorded_at);
CREATE INDEX idx_encounter_vitals_deleted_at ON encounter_vitals(deleted_at);

-- =============================================================
-- Permissions
-- =============================================================
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('ENCOUNTER_CREATE',    'Create Encounter',              'ENCOUNTER', 'CREATE', 'Start a clinical encounter from a checked-in appointment'),
    ('ENCOUNTER_VIEW',      'View Encounters',               'ENCOUNTER', 'READ',   'View clinical encounters'),
    ('ENCOUNTER_VIEW_ALL',  'View All Encounters',           'ENCOUNTER', 'READ',   'View all encounters across doctors and patients'),
    ('ENCOUNTER_UPDATE',    'Update Encounter',              'ENCOUNTER', 'UPDATE', 'Edit clinical documentation of an encounter'),
    ('ENCOUNTER_START',     'Start Encounter',               'ENCOUNTER', 'UPDATE', 'Progress a draft encounter to in progress'),
    ('ENCOUNTER_COMPLETE',  'Complete Encounter',            'ENCOUNTER', 'UPDATE', 'Complete a clinical encounter'),
    ('ENCOUNTER_CANCEL',    'Cancel Encounter',              'ENCOUNTER', 'DELETE', 'Cancel a clinical encounter with a reason'),
    ('VITALS_CREATE',       'Record Vitals',                 'VITALS',    'CREATE', 'Record patient vitals for an encounter'),
    ('VITALS_VIEW',         'View Vitals',                   'VITALS',    'READ',   'View vitals recorded for an encounter'),
    ('VITALS_UPDATE',       'Update Vitals',                 'VITALS',    'UPDATE', 'Correct vitals recorded for an encounter')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant all new permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('ENCOUNTER_CREATE', 'ENCOUNTER_VIEW', 'ENCOUNTER_VIEW_ALL', 'ENCOUNTER_UPDATE',
                 'ENCOUNTER_START', 'ENCOUNTER_COMPLETE', 'ENCOUNTER_CANCEL',
                 'VITALS_CREATE', 'VITALS_VIEW', 'VITALS_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: full clinical access to encounters and vitals
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('ENCOUNTER_CREATE', 'ENCOUNTER_VIEW', 'ENCOUNTER_UPDATE',
                 'ENCOUNTER_START', 'ENCOUNTER_COMPLETE', 'ENCOUNTER_CANCEL',
                 'VITALS_CREATE', 'VITALS_VIEW', 'VITALS_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: view encounters (front desk), no clinical documentation rights
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('ENCOUNTER_VIEW', 'ENCOUNTER_VIEW_ALL')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PATIENT: view own encounters and vitals
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN ('ENCOUNTER_VIEW', 'VITALS_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
