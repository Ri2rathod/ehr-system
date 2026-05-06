-- V6: Create MRN sequences table for concurrent-safe MRN generation
CREATE TABLE mrn_sequences (
    id BIGSERIAL PRIMARY KEY,

    sequence_year INT NOT NULL UNIQUE,

    last_number BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Initialize with current year
INSERT INTO mrn_sequences (sequence_year, last_number) VALUES (2026, 0);

-- Index for fast lookup
CREATE INDEX idx_mrn_sequences_year ON mrn_sequences(sequence_year);