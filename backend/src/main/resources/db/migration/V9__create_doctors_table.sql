-- V9: Create doctors table (clinical profile, separate from User auth identity)
-- One-to-one with users; soft-delete, multi-tenant, UUID external ID, doctor code

CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    user_id BIGINT UNIQUE,

    doctor_code VARCHAR(50) NOT NULL UNIQUE,

    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,

    display_name VARCHAR(255),

    gender VARCHAR(20),
    date_of_birth DATE,

    profile_photo_url TEXT,

    email VARCHAR(255),
    phone_country_code VARCHAR(10),
    phone_number VARCHAR(20),

    specialization VARCHAR(100),
    qualification VARCHAR(255),

    years_of_experience INT,

    consultation_fee DECIMAL(10,2),

    license_number VARCHAR(100),
    license_expiry_date DATE,

    biography TEXT,

    consultation_duration_minutes INT NOT NULL DEFAULT 15,

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    is_available_for_appointments BOOLEAN NOT NULL DEFAULT TRUE,

    tenant_id BIGINT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    version BIGINT DEFAULT 0,

    CONSTRAINT fk_doctors_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT fk_doctors_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT fk_doctors_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT chk_doctors_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'TRANSGENDER', 'UNKNOWN')),

    CONSTRAINT chk_doctors_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'ON_LEAVE', 'RETIRED')),

    CONSTRAINT chk_doctors_consultation_fee
        CHECK (consultation_fee IS NULL OR consultation_fee >= 0),

    CONSTRAINT chk_doctors_consultation_duration
        CHECK (consultation_duration_minutes > 0 AND consultation_duration_minutes <= 480)
);

-- Indexes
CREATE INDEX idx_doctors_uuid ON doctors(uuid);
CREATE INDEX idx_doctors_user_id ON doctors(user_id);
CREATE INDEX idx_doctors_doctor_code ON doctors(doctor_code);
CREATE INDEX idx_doctors_specialization ON doctors(specialization);
CREATE INDEX idx_doctors_status ON doctors(status);
CREATE INDEX idx_doctors_tenant_id ON doctors(tenant_id);
CREATE INDEX idx_doctors_deleted_at ON doctors(deleted_at);
CREATE INDEX idx_doctors_email ON doctors(email);
CREATE INDEX idx_doctors_phone ON doctors(phone_number);

-- Composite indexes for soft-delete + filtered queries
CREATE INDEX idx_doctors_deleted_status ON doctors(deleted_at, status);
CREATE INDEX idx_doctors_deleted_specialization ON doctors(deleted_at, specialization);
CREATE INDEX idx_doctors_deleted_available ON doctors(deleted_at, is_available_for_appointments);

-- Partial index for active available doctors
CREATE INDEX idx_doctors_active_available ON doctors(specialization) WHERE deleted_at IS NULL AND status = 'ACTIVE' AND is_available_for_appointments = TRUE;

-- Unique index for active doctors (no duplicate doctor codes for non-deleted)
CREATE UNIQUE INDEX idx_doctors_active_doctor_code ON doctors(doctor_code) WHERE deleted_at IS NULL;