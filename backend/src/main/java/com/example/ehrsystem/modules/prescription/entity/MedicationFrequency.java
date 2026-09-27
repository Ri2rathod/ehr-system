package com.example.ehrsystem.modules.prescription.entity;

/**
 * Controlled frequency model - not a scheduling engine.
 * CUSTOM requires additional instructions; AS_NEEDED (PRN) does not
 * imply a fixed frequency.
 */
public enum MedicationFrequency {
    ONCE_DAILY,
    TWICE_DAILY,
    THREE_TIMES_DAILY,
    FOUR_TIMES_DAILY,
    EVERY_MORNING,
    EVERY_EVENING,
    AT_BEDTIME,
    WEEKLY,
    AS_NEEDED,
    CUSTOM
}
