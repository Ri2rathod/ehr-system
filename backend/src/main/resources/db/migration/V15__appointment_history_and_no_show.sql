-- V15: Add no_show_at column + appointment_history table

-- Add no_show_at timestamp to appointments
ALTER TABLE appointments ADD COLUMN no_show_at TIMESTAMP;

-- Create appointment_history table for reschedule audit trail
CREATE TABLE appointment_history (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    appointment_id BIGINT NOT NULL,
    appointment_number VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_start_time TIMESTAMP,
    previous_end_time TIMESTAMP,
    previous_status VARCHAR(50),
    new_start_time TIMESTAMP,
    new_end_time TIMESTAMP,
    new_status VARCHAR(50),
    reason TEXT,
    performed_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_history_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE INDEX idx_appt_history_uuid ON appointment_history(uuid);
CREATE INDEX idx_appt_history_appointment ON appointment_history(appointment_id);
CREATE INDEX idx_appt_history_action ON appointment_history(action);
CREATE INDEX idx_appt_history_created ON appointment_history(created_at);
