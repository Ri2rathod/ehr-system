-- V13: Create doctor_availability_exceptions table
-- Represents date-specific overrides to the recurring weekly schedule
-- Supports full-day and partial-day exceptions

CREATE TABLE doctor_availability_exceptions (
    id              BIGSERIAL    PRIMARY KEY,

    uuid            UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    doctor_id       BIGINT       NOT NULL,

    exception_date  DATE         NOT NULL,

    exception_type  VARCHAR(30)  NOT NULL,

    start_time      TIME,
    end_time        TIME,

    is_full_day     BOOLEAN      NOT NULL DEFAULT TRUE,

    reason          TEXT,

    created_by      BIGINT,
    updated_by      BIGINT,

    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_doctor_exception_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,

    CONSTRAINT fk_doctor_exception_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT fk_doctor_exception_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Domain constraints
    CONSTRAINT chk_exception_type
        CHECK (exception_type IN (
            'VACATION', 'HOLIDAY', 'EMERGENCY_LEAVE',
            'PERSONAL_LEAVE', 'SICK_LEAVE', 'TRAINING', 'OTHER'
        )),

    -- Partial-day exceptions must have both start_time and end_time
    CONSTRAINT chk_exception_partial_day
        CHECK (is_full_day = TRUE OR (start_time IS NOT NULL AND end_time IS NOT NULL)),

    -- If both times are provided, start must be before end
    CONSTRAINT chk_exception_time_range
        CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time)
);

-- Indexes
CREATE INDEX idx_doctor_exception_uuid ON doctor_availability_exceptions(uuid);
CREATE INDEX idx_doctor_exception_doctor_id ON doctor_availability_exceptions(doctor_id);
CREATE INDEX idx_doctor_exception_date ON doctor_availability_exceptions(exception_date);
CREATE INDEX idx_doctor_exception_type ON doctor_availability_exceptions(exception_type);

-- Composite: doctor + date (primary query path for slot generation)
CREATE INDEX idx_doctor_exception_doctor_date
    ON doctor_availability_exceptions(doctor_id, exception_date);
