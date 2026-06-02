-- V11: Create appointments and appointment_sequences tables

-- Create sequence table for concurrent-safe appointment number generation
CREATE TABLE appointment_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointment_sequences_year ON appointment_sequences(sequence_year);

-- Create appointments table
CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    appointment_number VARCHAR(50) NOT NULL UNIQUE,

    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,

    appointment_date DATE NOT NULL,

    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    visit_type VARCHAR(50) NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',

    reason_for_visit TEXT,

    notes TEXT,

    cancellation_reason TEXT,

    checked_in_at TIMESTAMP,
    in_progress_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,

    tenant_id BIGINT,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_appointment_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_appointment_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES doctors(id)
);

-- Required Indexes
CREATE INDEX idx_appointment_uuid ON appointments(uuid);
CREATE INDEX idx_appointment_number ON appointments(appointment_number);
CREATE INDEX idx_appointment_patient ON appointments(patient_id);
CREATE INDEX idx_appointment_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointment_date ON appointments(appointment_date);
CREATE INDEX idx_appointment_status ON appointments(status);
CREATE INDEX idx_appointment_deleted ON appointments(deleted_at);
