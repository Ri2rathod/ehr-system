-- V12: Create doctor_availability table
-- Represents recurring weekly schedule with WORK/BREAK types
-- Supports effective date ranges for schedule versioning

CREATE TABLE doctor_availability (
    id          BIGSERIAL    PRIMARY KEY,

    uuid        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    doctor_id   BIGINT       NOT NULL,

    day_of_week VARCHAR(10)  NOT NULL,

    schedule_type VARCHAR(10) NOT NULL,

    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,

    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    effective_from DATE,
    effective_until DATE,

    created_by  BIGINT,
    updated_by  BIGINT,

    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_doctor_availability_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,

    CONSTRAINT fk_doctor_availability_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT fk_doctor_availability_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Domain constraints
    CONSTRAINT chk_availability_day_of_week
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),

    CONSTRAINT chk_availability_schedule_type
        CHECK (schedule_type IN ('WORK', 'BREAK')),

    CONSTRAINT chk_availability_time_range
        CHECK (start_time < end_time),

    CONSTRAINT chk_availability_effective_range
        CHECK (effective_until IS NULL OR effective_from IS NULL OR effective_from <= effective_until)
);

-- Indexes for query performance
CREATE INDEX idx_doctor_avail_uuid ON doctor_availability(uuid);
CREATE INDEX idx_doctor_avail_doctor_id ON doctor_availability(doctor_id);
CREATE INDEX idx_doctor_avail_day ON doctor_availability(day_of_week);
CREATE INDEX idx_doctor_avail_type ON doctor_availability(schedule_type);
CREATE INDEX idx_doctor_avail_active ON doctor_availability(is_active);

-- Composite index: doctor + day + active (primary query path)
CREATE INDEX idx_doctor_avail_doctor_day_active
    ON doctor_availability(doctor_id, day_of_week, is_active);

-- Composite index: doctor + effective range (schedule versioning)
CREATE INDEX idx_doctor_avail_effective
    ON doctor_availability(doctor_id, effective_from, effective_until);
