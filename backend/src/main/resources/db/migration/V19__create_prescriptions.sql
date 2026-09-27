-- V19: Medication catalog + prescription orders (child clinical domain of encounters)
-- Plus prescription_sequences for concurrent-safe PRES-YYYY-NNNNNN number generation
-- and prescription/medication permissions with role assignments.

-- =============================================================
-- Number generation (mirrors encounter/appointment sequence pattern)
-- =============================================================
CREATE TABLE prescription_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prescription_sequences_year ON prescription_sequences(sequence_year);

-- =============================================================
-- Medication catalog (patient/encounter independent reference data)
-- =============================================================
CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    code VARCHAR(50),
    code_system VARCHAR(50),

    generic_name VARCHAR(300) NOT NULL,
    brand_name VARCHAR(300),

    strength NUMERIC(10,3),
    strength_unit VARCHAR(20),

    dosage_form VARCHAR(30),
    route VARCHAR(30),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_medication_strength
        CHECK (strength IS NULL OR strength >= 0),

    CONSTRAINT ck_medication_strength_unit
        CHECK (strength_unit IS NULL OR strength_unit IN
            ('MG', 'MCG', 'G', 'ML', 'IU', 'MEQ', 'PERCENT', 'PUFF', 'DROP', 'UNIT', 'OTHER')),

    CONSTRAINT ck_medication_dosage_form
        CHECK (dosage_form IS NULL OR dosage_form IN
            ('TABLET', 'CAPSULE', 'SYRUP', 'SOLUTION', 'CREAM', 'OINTMENT',
             'INJECTION', 'DROPS', 'INHALER', 'OTHER')),

    CONSTRAINT ck_medication_route
        CHECK (route IS NULL OR route IN
            ('ORAL', 'TOPICAL', 'INTRAVENOUS', 'INTRAMUSCULAR', 'SUBCUTANEOUS',
             'INHALATION', 'OPHTHALMIC', 'OTIC', 'NASAL', 'RECTAL', 'OTHER'))
);

-- Terminology-ready: one active catalog entry per (code system, code)
CREATE UNIQUE INDEX uq_medication_code_active
    ON medications(code_system, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;

-- Required Indexes
CREATE INDEX idx_medication_uuid ON medications(uuid);
CREATE INDEX idx_medication_code ON medications(code);
CREATE INDEX idx_medication_generic_name ON medications(generic_name);
CREATE INDEX idx_medication_brand_name ON medications(brand_name);
CREATE INDEX idx_medication_is_active ON medications(is_active);
CREATE INDEX idx_medication_deleted_at ON medications(deleted_at);
-- Medication search: active catalog entries by name
CREATE INDEX idx_medication_search ON medications(generic_name, brand_name)
    WHERE deleted_at IS NULL AND is_active = TRUE;

-- =============================================================
-- Prescriptions (clinical order belonging to an Encounter)
-- =============================================================
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    prescription_number VARCHAR(50) NOT NULL UNIQUE,

    encounter_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    prescribed_at TIMESTAMP NOT NULL,

    notes TEXT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_prescription_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounters(id),

    CONSTRAINT fk_prescription_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_prescription_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id),

    CONSTRAINT ck_prescription_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'VOID'))
);

-- Required Indexes
CREATE INDEX idx_prescription_uuid ON prescriptions(uuid);
CREATE INDEX idx_prescription_number ON prescriptions(prescription_number);
CREATE INDEX idx_prescription_encounter ON prescriptions(encounter_id, deleted_at);
CREATE INDEX idx_prescription_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescription_doctor ON prescriptions(doctor_id);
CREATE INDEX idx_prescription_status ON prescriptions(status);
CREATE INDEX idx_prescription_prescribed_at ON prescriptions(prescribed_at);
CREATE INDEX idx_prescription_deleted_at ON prescriptions(deleted_at);
-- Optimize: Encounter -> active prescription
CREATE INDEX idx_prescription_encounter_status ON prescriptions(encounter_id, status)
    WHERE deleted_at IS NULL;

-- =============================================================
-- Prescription items (structured medication instructions)
-- Medication identity is immutable per item; changes are made by
-- discontinue + create new item (preserves clinical history).
-- =============================================================
CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    prescription_id BIGINT NOT NULL,
    medication_id BIGINT NOT NULL,

    dose NUMERIC(10,3) NOT NULL,
    dose_unit VARCHAR(20) NOT NULL,

    route VARCHAR(30) NOT NULL,

    frequency VARCHAR(30) NOT NULL,
    frequency_value INT,
    frequency_unit VARCHAR(20),

    duration INT,
    duration_unit VARCHAR(20),

    quantity INT,
    quantity_unit VARCHAR(20),

    refills INT NOT NULL DEFAULT 0,

    instructions TEXT,

    start_date DATE,
    end_date DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_prescription_item_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id),

    CONSTRAINT fk_prescription_item_medication
        FOREIGN KEY (medication_id)
        REFERENCES medications(id),

    CONSTRAINT ck_rx_item_dose
        CHECK (dose >= 0),

    CONSTRAINT ck_rx_item_dose_unit
        CHECK (dose_unit IN
            ('MG', 'MCG', 'G', 'ML', 'IU', 'MEQ', 'PERCENT', 'PUFF', 'DROP', 'UNIT', 'OTHER')),

    CONSTRAINT ck_rx_item_route
        CHECK (route IN
            ('ORAL', 'TOPICAL', 'INTRAVENOUS', 'INTRAMUSCULAR', 'SUBCUTANEOUS',
             'INHALATION', 'OPHTHALMIC', 'OTIC', 'NASAL', 'RECTAL', 'OTHER')),

    CONSTRAINT ck_rx_item_frequency
        CHECK (frequency IN
            ('ONCE_DAILY', 'TWICE_DAILY', 'THREE_TIMES_DAILY', 'FOUR_TIMES_DAILY',
             'EVERY_MORNING', 'EVERY_EVENING', 'AT_BEDTIME', 'WEEKLY', 'AS_NEEDED', 'CUSTOM')),

    -- Custom frequency detail: value and unit must appear together
    CONSTRAINT ck_rx_item_frequency_pair
        CHECK ((frequency_value IS NULL AND frequency_unit IS NULL)
            OR (frequency_value IS NOT NULL AND frequency_unit IS NOT NULL)),

    CONSTRAINT ck_rx_item_frequency_value
        CHECK (frequency_value IS NULL OR frequency_value > 0),

    CONSTRAINT ck_rx_item_frequency_unit
        CHECK (frequency_unit IS NULL OR frequency_unit IN ('HOURS', 'DAYS')),

    -- Duration reuses the DurationUnit concept from Treatment/Care Plan
    CONSTRAINT ck_rx_item_duration_pair
        CHECK ((duration IS NULL AND duration_unit IS NULL)
            OR (duration IS NOT NULL AND duration_unit IS NOT NULL)),

    CONSTRAINT ck_rx_item_duration
        CHECK (duration IS NULL OR duration > 0),

    CONSTRAINT ck_rx_item_duration_unit
        CHECK (duration_unit IS NULL OR duration_unit IN ('DAYS', 'WEEKS', 'MONTHS')),

    CONSTRAINT ck_rx_item_quantity_pair
        CHECK ((quantity IS NULL AND quantity_unit IS NULL)
            OR (quantity IS NOT NULL AND quantity_unit IS NOT NULL)),

    CONSTRAINT ck_rx_item_quantity
        CHECK (quantity IS NULL OR quantity >= 0),

    CONSTRAINT ck_rx_item_quantity_unit
        CHECK (quantity_unit IS NULL OR quantity_unit IN
            ('TABLETS', 'CAPSULES', 'ML', 'G', 'PUFFS', 'DROPS', 'UNITS', 'OTHER')),

    CONSTRAINT ck_rx_item_refills
        CHECK (refills >= 0),

    CONSTRAINT ck_rx_item_dates
        CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),

    CONSTRAINT ck_rx_item_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'DISCONTINUED'))
);

-- Required Indexes
CREATE INDEX idx_prescription_item_uuid ON prescription_items(uuid);
CREATE INDEX idx_prescription_item_prescription ON prescription_items(prescription_id, deleted_at);
CREATE INDEX idx_prescription_item_medication ON prescription_items(medication_id);
CREATE INDEX idx_prescription_item_status ON prescription_items(status);
CREATE INDEX idx_prescription_item_deleted_at ON prescription_items(deleted_at);

-- =============================================================
-- Permissions (16 recommended + 2 delete permissions following the
-- project's DELETE = soft-delete convention)
-- =============================================================
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('MEDICATION_VIEW',          'View Medications',            'MEDICATION',   'READ',   'Search and view the medication catalog'),
    ('MEDICATION_CREATE',        'Create Medication',           'MEDICATION',   'CREATE', 'Add a medication to the catalog'),
    ('MEDICATION_UPDATE',        'Update Medication',           'MEDICATION',   'UPDATE', 'Edit a medication catalog entry'),
    ('MEDICATION_DEACTIVATE',    'Deactivate Medication',       'MEDICATION',   'DELETE', 'Remove a medication from the active catalog'),
    ('PRESCRIPTION_VIEW',        'View Prescriptions',          'PRESCRIPTION', 'READ',   'View prescriptions recorded for an encounter'),
    ('PRESCRIPTION_CREATE',      'Create Prescription',         'PRESCRIPTION', 'CREATE', 'Create a prescription for an encounter'),
    ('PRESCRIPTION_UPDATE',      'Update Prescription',         'PRESCRIPTION', 'UPDATE', 'Edit a prescription'),
    ('PRESCRIPTION_ACTIVATE',    'Activate Prescription',       'PRESCRIPTION', 'UPDATE', 'Activate a draft prescription'),
    ('PRESCRIPTION_COMPLETE',    'Complete Prescription',       'PRESCRIPTION', 'UPDATE', 'Complete an active prescription'),
    ('PRESCRIPTION_CANCEL',      'Cancel Prescription',         'PRESCRIPTION', 'DELETE', 'Cancel a prescription'),
    ('PRESCRIPTION_VOID',        'Void Prescription',           'PRESCRIPTION', 'DELETE', 'Void an active prescription'),
    ('PRESCRIPTION_DELETE',      'Delete Prescription',         'PRESCRIPTION', 'DELETE', 'Remove a prescription from the encounter'),
    ('PRESCRIPTION_ITEM_VIEW',   'View Prescription Items',     'PRESCRIPTION', 'READ',   'View medication items of a prescription'),
    ('PRESCRIPTION_ITEM_CREATE', 'Add Prescription Item',       'PRESCRIPTION', 'CREATE', 'Add a medication to a prescription'),
    ('PRESCRIPTION_ITEM_UPDATE', 'Update Prescription Item',    'PRESCRIPTION', 'UPDATE', 'Edit a prescription medication instruction'),
    ('PRESCRIPTION_ITEM_DISCONTINUE', 'Discontinue Prescription Item', 'PRESCRIPTION', 'UPDATE', 'Discontinue a medication on a prescription'),
    ('PRESCRIPTION_ITEM_CANCEL', 'Cancel Prescription Item',    'PRESCRIPTION', 'DELETE', 'Cancel a medication on a prescription'),
    ('PRESCRIPTION_ITEM_DELETE', 'Delete Prescription Item',    'PRESCRIPTION', 'DELETE', 'Remove a medication item from the prescription')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('MEDICATION_VIEW', 'MEDICATION_CREATE', 'MEDICATION_UPDATE', 'MEDICATION_DEACTIVATE',
                 'PRESCRIPTION_VIEW', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_ACTIVATE',
                 'PRESCRIPTION_COMPLETE', 'PRESCRIPTION_CANCEL', 'PRESCRIPTION_VOID', 'PRESCRIPTION_DELETE',
                 'PRESCRIPTION_ITEM_VIEW', 'PRESCRIPTION_ITEM_CREATE', 'PRESCRIPTION_ITEM_UPDATE',
                 'PRESCRIPTION_ITEM_DISCONTINUE', 'PRESCRIPTION_ITEM_CANCEL', 'PRESCRIPTION_ITEM_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: prescribe (catalog read only - catalog management stays admin-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('MEDICATION_VIEW',
                 'PRESCRIPTION_VIEW', 'PRESCRIPTION_CREATE', 'PRESCRIPTION_UPDATE', 'PRESCRIPTION_ACTIVATE',
                 'PRESCRIPTION_COMPLETE', 'PRESCRIPTION_CANCEL', 'PRESCRIPTION_VOID', 'PRESCRIPTION_DELETE',
                 'PRESCRIPTION_ITEM_VIEW', 'PRESCRIPTION_ITEM_CREATE', 'PRESCRIPTION_ITEM_UPDATE',
                 'PRESCRIPTION_ITEM_DISCONTINUE', 'PRESCRIPTION_ITEM_CANCEL', 'PRESCRIPTION_ITEM_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: view prescriptions only (no clinical mutation rights)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('PRESCRIPTION_VIEW', 'PRESCRIPTION_ITEM_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PATIENT: view own prescriptions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN ('PRESCRIPTION_VIEW', 'PRESCRIPTION_ITEM_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
