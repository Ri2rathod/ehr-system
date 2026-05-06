-- V5: Create patients table for healthcare demographics (separate from users/auth)
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    user_id BIGINT UNIQUE,

    mrn VARCHAR(50) NOT NULL UNIQUE,

    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,

    display_name VARCHAR(255),

    gender VARCHAR(20),
    date_of_birth DATE,

    blood_group VARCHAR(10),

    marital_status VARCHAR(50),

    profile_photo_url TEXT,

    email VARCHAR(255),
    phone_country_code VARCHAR(10),
    phone_number VARCHAR(20),

    emergency_contact_name VARCHAR(255),
    emergency_contact_relationship VARCHAR(100),
    emergency_contact_phone VARCHAR(20),

    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_deceased BOOLEAN NOT NULL DEFAULT FALSE,
    deceased_at TIMESTAMP,

    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    insurance_provider VARCHAR(255),
    insurance_policy_number VARCHAR(100),

    allergies TEXT,
    chronic_conditions TEXT,
    notes TEXT,

    tenant_id BIGINT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_patients_uuid ON patients(uuid);
CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_patients_first_name ON patients(first_name);
CREATE INDEX idx_patients_last_name ON patients(last_name);
CREATE INDEX idx_patients_phone_number ON patients(phone_number);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_status ON patients(status);
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);
CREATE INDEX idx_patients_deleted_at ON patients(deleted_at);