-- V7: Add DB-level integrity constraints and composite indexes for patient table
-- Combined migration for enum constraints + query optimization indexes

-- ============================================================
-- Add version column for optimistic locking (not created by JPA validate mode)
-- ============================================================

ALTER TABLE patients ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================================
-- CHECK constraints for enum-like fields (defense in depth)
-- ============================================================

ALTER TABLE patients ADD CONSTRAINT chk_patients_gender
    CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'TRANSGENDER', 'UNKNOWN'));

ALTER TABLE patients ADD CONSTRAINT chk_patients_blood_group
    CHECK (blood_group IS NULL OR blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE', 'UNKNOWN'));

ALTER TABLE patients ADD CONSTRAINT chk_patients_marital_status
    CHECK (marital_status IS NULL OR marital_status IN ('SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED', 'SEPARATED', 'UNKNOWN'));

ALTER TABLE patients ADD CONSTRAINT chk_patients_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'BLOCKED'));

-- ============================================================
-- Composite indexes for soft-delete + filtered queries
-- ============================================================

CREATE INDEX idx_patients_deleted_status ON patients(deleted_at, status);
CREATE INDEX idx_patients_deleted_mrn ON patients(deleted_at, mrn);
CREATE INDEX idx_patients_deleted_uuid ON patients(deleted_at, uuid);
CREATE INDEX idx_patients_deleted_tenant ON patients(deleted_at, tenant_id);
CREATE INDEX idx_patients_deleted_created ON patients(deleted_at, created_at DESC);

-- ============================================================
-- Partial index for active patients (most common query)
-- ============================================================

CREATE INDEX idx_patients_active_mrn ON patients(mrn) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_active_uuid ON patients(uuid) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_active_status ON patients(status) WHERE deleted_at IS NULL;

-- ============================================================
-- FK constraint improvements
-- ============================================================

ALTER TABLE patients ADD CONSTRAINT fk_patients_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE patients ADD CONSTRAINT fk_patients_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;