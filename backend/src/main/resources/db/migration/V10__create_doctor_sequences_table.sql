-- V10: Create doctor_sequences table for concurrent-safe DOC-YYYY-NNNNNN code generation

CREATE TABLE doctor_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_year INT NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO doctor_sequences (sequence_year, last_number) VALUES (2026, 0);

CREATE INDEX idx_doctor_sequences_year ON doctor_sequences(sequence_year);