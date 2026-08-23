package com.example.ehrsystem.modules.scheduling.entity;

/**
 * Schedule block type within a doctor's weekly availability.
 * <p>
 * WORK  – the doctor is available for appointments.
 * BREAK – the doctor is unavailable (lunch, rest, etc.).
 * </p>
 */
public enum ScheduleType {
    WORK,
    BREAK
}
