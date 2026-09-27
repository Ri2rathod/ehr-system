-- V20: Lab tests catalog, lab orders, specimens, lab results and result values
-- Plus lab_order_sequences for concurrent-safe LAB-YYYY-NNNNNN number generation
-- and lab permissions with role assignments.

-- =============================================================
-- Number generation (mirrors encounter/appointment/prescription pattern)
-- =============================================================
CREATE TABLE lab_order_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lab_order_sequences_year ON lab_order_sequences(sequence_year);

-- =============================================================
-- Lab test catalog (patient/encounter independent reference data)
-- =============================================================
CREATE TABLE lab_tests (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL,
    code_system VARCHAR(50),

    name VARCHAR(300) NOT NULL,
    short_name VARCHAR(100),
    description TEXT,
    category VARCHAR(100),

    specimen_type VARCHAR(30),
    result_type VARCHAR(20) NOT NULL,

    unit VARCHAR(50),
    default_reference_low NUMERIC(14,4),
    default_reference_high NUMERIC(14,4),
    default_reference_text VARCHAR(300),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_lab_test_result_type
        CHECK (result_type IN ('NUMERIC', 'TEXT', 'QUALITATIVE', 'CODED')),

    CONSTRAINT ck_lab_test_specimen_type
        CHECK (specimen_type IS NULL OR specimen_type IN
            ('BLOOD', 'SERUM', 'PLASMA', 'URINE', 'STOOL', 'SWAB',
             'SPUTUM', 'CSF', 'SALIVA', 'TISSUE', 'OTHER')),

    CONSTRAINT ck_lab_test_default_reference_pair
        CHECK (default_reference_low IS NULL OR default_reference_high IS NULL
            OR default_reference_low <= default_reference_high)
);

-- Terminology-ready: one active catalog entry per (code system, code)
CREATE UNIQUE INDEX uq_lab_test_code_active
    ON lab_tests(code_system, code)
    WHERE deleted_at IS NULL;

-- Required Indexes
CREATE INDEX idx_lab_test_uuid ON lab_tests(uuid);
CREATE INDEX idx_lab_test_code ON lab_tests(code);
CREATE INDEX idx_lab_test_name ON lab_tests(name);
CREATE INDEX idx_lab_test_category ON lab_tests(category);
CREATE INDEX idx_lab_test_is_active ON lab_tests(is_active);
CREATE INDEX idx_lab_test_deleted_at ON lab_tests(deleted_at);
-- Lab test search: active catalog entries
CREATE INDEX idx_lab_test_search ON lab_tests(name, code)
    WHERE deleted_at IS NULL AND is_active = TRUE;

-- =============================================================
-- Lab orders (clinical order belonging to an Encounter)
-- patient/doctor are derived through the encounter, not duplicated here.
-- =============================================================
CREATE TABLE lab_orders (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    order_number VARCHAR(50) NOT NULL UNIQUE,

    encounter_id BIGINT NOT NULL,

    priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    instructions TEXT,

    ordered_at TIMESTAMP,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lab_order_encounter
        FOREIGN KEY (encounter_id)
        REFERENCES encounters(id),

    CONSTRAINT ck_lab_order_priority
        CHECK (priority IN ('ROUTINE', 'URGENT', 'STAT')),

    CONSTRAINT ck_lab_order_status
        CHECK (status IN ('DRAFT', 'ORDERED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Required Indexes
CREATE INDEX idx_lab_order_uuid ON lab_orders(uuid);
CREATE INDEX idx_lab_order_number ON lab_orders(order_number);
CREATE INDEX idx_lab_order_encounter ON lab_orders(encounter_id, deleted_at);
CREATE INDEX idx_lab_order_status ON lab_orders(status);
CREATE INDEX idx_lab_order_ordered_at ON lab_orders(ordered_at);
CREATE INDEX idx_lab_order_deleted_at ON lab_orders(deleted_at);
-- Optimize: Encounter -> active lab order
CREATE INDEX idx_lab_order_encounter_status ON lab_orders(encounter_id, status)
    WHERE deleted_at IS NULL;

-- =============================================================
-- Lab order items (ordered lab tests; a test can only be ordered
-- once per order while the item is active)
-- =============================================================
CREATE TABLE lab_order_items (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lab_order_id BIGINT NOT NULL,
    lab_test_id BIGINT NOT NULL,

    instructions TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'ORDERED',

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lab_order_item_order
        FOREIGN KEY (lab_order_id)
        REFERENCES lab_orders(id),

    CONSTRAINT fk_lab_order_item_test
        FOREIGN KEY (lab_test_id)
        REFERENCES lab_tests(id),

    CONSTRAINT ck_lab_order_item_status
        CHECK (status IN ('ORDERED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- A lab test can only appear once per order while its item is active
CREATE UNIQUE INDEX uq_lab_order_item_test
    ON lab_order_items(lab_order_id, lab_test_id)
    WHERE deleted_at IS NULL;

-- Required Indexes
CREATE INDEX idx_lab_order_item_uuid ON lab_order_items(uuid);
CREATE INDEX idx_lab_order_item_order ON lab_order_items(lab_order_id, deleted_at);
CREATE INDEX idx_lab_order_item_test ON lab_order_items(lab_test_id);
CREATE INDEX idx_lab_order_item_status ON lab_order_items(status);
CREATE INDEX idx_lab_order_item_deleted_at ON lab_order_items(deleted_at);

-- =============================================================
-- Specimens (belong to a Lab Order; lifecycle is independent of
-- the encounter once the order exists)
-- =============================================================
CREATE TABLE specimens (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lab_order_id BIGINT NOT NULL,

    specimen_type VARCHAR(30) NOT NULL,
    specimen_identifier VARCHAR(100),

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_COLLECTION',

    collected_at TIMESTAMP,
    collected_by BIGINT,
    received_at TIMESTAMP,
    received_by BIGINT,

    rejection_reason VARCHAR(500),
    notes TEXT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_specimen_order
        FOREIGN KEY (lab_order_id)
        REFERENCES lab_orders(id),

    CONSTRAINT ck_specimen_type
        CHECK (specimen_type IN
            ('BLOOD', 'SERUM', 'PLASMA', 'URINE', 'STOOL', 'SWAB',
             'SPUTUM', 'CSF', 'SALIVA', 'TISSUE', 'OTHER')),

    CONSTRAINT ck_specimen_status
        CHECK (status IN ('PENDING_COLLECTION', 'COLLECTED', 'RECEIVED', 'REJECTED', 'CANCELLED')),

    -- Rejection reason is mandatory exactly when the specimen is rejected
    CONSTRAINT ck_specimen_rejection_reason
        CHECK ((status = 'REJECTED' AND rejection_reason IS NOT NULL)
            OR status <> 'REJECTED'),

    CONSTRAINT ck_specimen_collection
        CHECK ((status IN ('COLLECTED', 'RECEIVED', 'REJECTED') AND collected_at IS NOT NULL)
            OR status NOT IN ('COLLECTED', 'RECEIVED', 'REJECTED')),

    CONSTRAINT ck_specimen_received
        CHECK ((status = 'RECEIVED' AND received_at IS NOT NULL)
            OR status <> 'RECEIVED')
);

-- Required Indexes
CREATE INDEX idx_specimen_uuid ON specimens(uuid);
CREATE INDEX idx_specimen_order ON specimens(lab_order_id, deleted_at);
CREATE INDEX idx_specimen_status ON specimens(status);
CREATE INDEX idx_specimen_type ON specimens(specimen_type);
CREATE INDEX idx_specimen_collected_at ON specimens(collected_at);
CREATE INDEX idx_specimen_received_at ON specimens(received_at);
CREATE INDEX idx_specimen_deleted_at ON specimens(deleted_at);

-- =============================================================
-- Lab results (belong to a LabOrderItem + Specimen; values hold the
-- measured components. Finalized results are never deleted.)
-- =============================================================
CREATE TABLE lab_results (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lab_order_item_id BIGINT NOT NULL,
    specimen_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PRELIMINARY',

    resulted_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    verified_by BIGINT,

    comments TEXT,
    correction_reason VARCHAR(1000),

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lab_result_item
        FOREIGN KEY (lab_order_item_id)
        REFERENCES lab_order_items(id),

    CONSTRAINT fk_lab_result_specimen
        FOREIGN KEY (specimen_id)
        REFERENCES specimens(id),

    CONSTRAINT ck_lab_result_status
        CHECK (status IN ('PRELIMINARY', 'FINAL', 'CORRECTED', 'CANCELLED')),

    -- Correction reason is mandatory exactly when the result was corrected
    CONSTRAINT ck_lab_result_correction_reason
        CHECK ((status = 'CORRECTED' AND correction_reason IS NOT NULL)
            OR status <> 'CORRECTED'),

    -- Verified results carry verification metadata
    CONSTRAINT ck_lab_result_verified
        CHECK ((status IN ('FINAL', 'CORRECTED') AND verified_at IS NOT NULL)
            OR status NOT IN ('FINAL', 'CORRECTED'))
);

-- Required Indexes
CREATE INDEX idx_lab_result_uuid ON lab_results(uuid);
CREATE INDEX idx_lab_result_item ON lab_results(lab_order_item_id, deleted_at);
CREATE INDEX idx_lab_result_specimen ON lab_results(specimen_id);
CREATE INDEX idx_lab_result_status ON lab_results(status);
CREATE INDEX idx_lab_result_resulted_at ON lab_results(resulted_at);
CREATE INDEX idx_lab_result_verified_at ON lab_results(verified_at);
CREATE INDEX idx_lab_result_deleted_at ON lab_results(deleted_at);

-- =============================================================
-- Lab result values (single numeric/text/code representation,
-- reference range copied from the catalog at result time)
-- =============================================================
CREATE TABLE lab_result_values (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    lab_result_id BIGINT NOT NULL,
    lab_test_id BIGINT NOT NULL,

    value_numeric NUMERIC(14,4),
    value_text VARCHAR(500),
    value_code VARCHAR(100),

    unit VARCHAR(50),

    reference_low NUMERIC(14,4),
    reference_high NUMERIC(14,4),
    reference_text VARCHAR(300),

    abnormal_flag VARCHAR(30),

    notes TEXT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_lab_result_value_result
        FOREIGN KEY (lab_result_id)
        REFERENCES lab_results(id),

    CONSTRAINT fk_lab_result_value_test
        FOREIGN KEY (lab_test_id)
        REFERENCES lab_tests(id),

    -- Exactly one representation is stored
    CONSTRAINT ck_lab_result_value_single_representation
        CHECK (
            (CASE WHEN value_numeric IS NOT NULL THEN 1 ELSE 0 END)
          + (CASE WHEN value_text IS NOT NULL THEN 1 ELSE 0 END)
          + (CASE WHEN value_code IS NOT NULL THEN 1 ELSE 0 END) = 1
        ),

    CONSTRAINT ck_lab_result_value_reference_pair
        CHECK (reference_low IS NULL OR reference_high IS NULL
            OR reference_low <= reference_high),

    CONSTRAINT ck_lab_result_value_abnormal_flag
        CHECK (abnormal_flag IS NULL OR abnormal_flag IN
            ('LOW', 'HIGH', 'CRITICAL_LOW', 'CRITICAL_HIGH', 'ABNORMAL',
             'POSITIVE', 'NEGATIVE', 'NORMAL'))
);

-- Required Indexes
CREATE INDEX idx_lab_result_value_uuid ON lab_result_values(uuid);
CREATE INDEX idx_lab_result_value_result ON lab_result_values(lab_result_id, deleted_at);
CREATE INDEX idx_lab_result_value_test ON lab_result_values(lab_test_id);
CREATE INDEX idx_lab_result_value_abnormal_flag ON lab_result_values(abnormal_flag);
CREATE INDEX idx_lab_result_value_deleted_at ON lab_result_values(deleted_at);

-- =============================================================
-- Permissions (16 recommended)
-- =============================================================
INSERT INTO permissions (code, name, module, action, description) VALUES
    ('LAB_TEST_VIEW',       'View Lab Tests',          'LAB_TEST',   'READ',   'Search and view the lab test catalog'),
    ('LAB_TEST_MANAGE',     'Manage Lab Tests',        'LAB_TEST',   'CREATE', 'Create, edit or remove lab test catalog entries'),
    ('LAB_ORDER_VIEW',      'View Lab Orders',         'LAB_ORDER',  'READ',   'View lab orders recorded for an encounter'),
    ('LAB_ORDER_CREATE',    'Create Lab Order',        'LAB_ORDER',  'CREATE', 'Create a lab order and add tests to it'),
    ('LAB_ORDER_UPDATE',    'Update Lab Order',        'LAB_ORDER',  'UPDATE', 'Edit a lab order and progress its workflow'),
    ('LAB_ORDER_CANCEL',    'Cancel Lab Order',        'LAB_ORDER',  'DELETE', 'Cancel or remove a lab order'),
    ('SPECIMEN_VIEW',       'View Specimens',          'SPECIMEN',   'READ',   'View specimens collected for a lab order'),
    ('SPECIMEN_COLLECT',    'Collect Specimen',        'SPECIMEN',   'CREATE', 'Add and collect a specimen for a lab order'),
    ('SPECIMEN_RECEIVE',    'Receive Specimen',        'SPECIMEN',   'UPDATE', 'Mark a collected specimen as received'),
    ('SPECIMEN_REJECT',     'Reject Specimen',         'SPECIMEN',   'DELETE', 'Reject an unsuitable specimen'),
    ('LAB_RESULT_VIEW',     'View Lab Results',        'LAB_RESULT', 'READ',   'View lab results recorded for a lab order'),
    ('LAB_RESULT_CREATE',   'Create Lab Result',       'LAB_RESULT', 'CREATE', 'Enter a preliminary lab result'),
    ('LAB_RESULT_UPDATE',   'Update Lab Result',       'LAB_RESULT', 'UPDATE', 'Edit a preliminary lab result'),
    ('LAB_RESULT_FINALIZE', 'Finalize Lab Result',     'LAB_RESULT', 'UPDATE', 'Finalize a preliminary lab result'),
    ('LAB_RESULT_CORRECT',  'Correct Lab Result',      'LAB_RESULT', 'UPDATE', 'Correct a finalized lab result'),
    ('LAB_RESULT_CANCEL',   'Cancel Lab Result',       'LAB_RESULT', 'DELETE', 'Cancel a preliminary lab result')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: grant everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    p.id
FROM permissions p
WHERE p.code IN ('LAB_TEST_VIEW', 'LAB_TEST_MANAGE',
                 'LAB_ORDER_VIEW', 'LAB_ORDER_CREATE', 'LAB_ORDER_UPDATE', 'LAB_ORDER_CANCEL',
                 'SPECIMEN_VIEW', 'SPECIMEN_COLLECT', 'SPECIMEN_RECEIVE', 'SPECIMEN_REJECT',
                 'LAB_RESULT_VIEW', 'LAB_RESULT_CREATE', 'LAB_RESULT_UPDATE',
                 'LAB_RESULT_FINALIZE', 'LAB_RESULT_CORRECT', 'LAB_RESULT_CANCEL')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DOCTOR: order tests and follow results (catalog read only; specimen
-- handling and result entry belong to lab operations, not the ordering role)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'DOCTOR'),
    p.id
FROM permissions p
WHERE p.code IN ('LAB_TEST_VIEW',
                 'LAB_ORDER_VIEW', 'LAB_ORDER_CREATE', 'LAB_ORDER_UPDATE', 'LAB_ORDER_CANCEL',
                 'SPECIMEN_VIEW', 'LAB_RESULT_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RECEPTIONIST: view lab workflow only (no clinical mutation rights)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'RECEPTIONIST'),
    p.id
FROM permissions p
WHERE p.code IN ('LAB_ORDER_VIEW', 'SPECIMEN_VIEW', 'LAB_RESULT_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PATIENT: view own lab orders and results
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'PATIENT'),
    p.id
FROM permissions p
WHERE p.code IN ('LAB_ORDER_VIEW', 'LAB_RESULT_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
