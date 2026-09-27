package com.example.ehrsystem.modules.prescription.entity;

/**
 * Prescription lifecycle. DRAFT -> ACTIVE -> COMPLETED;
 * DRAFT/ACTIVE -> CANCELLED; ACTIVE -> VOID.
 * Terminal: COMPLETED, CANCELLED, VOID.
 */
public enum PrescriptionStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    VOID
}
