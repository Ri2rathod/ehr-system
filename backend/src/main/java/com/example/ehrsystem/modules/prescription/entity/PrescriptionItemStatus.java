package com.example.ehrsystem.modules.prescription.entity;

/**
 * Independent item lifecycle - one discontinued medication does not
 * cancel the whole prescription.
 */
public enum PrescriptionItemStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    DISCONTINUED
}
