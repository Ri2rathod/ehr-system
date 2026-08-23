package com.example.ehrsystem.modules.scheduling.entity;

/**
 * Days of the week for recurring availability.
 * Mirrors {@link java.time.DayOfWeek} values but stored as VARCHAR in the database.
 */
public enum DayOfWeekEnum {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    /**
     * Converts a {@link java.time.DayOfWeek} to this enum.
     */
    public static DayOfWeekEnum from(java.time.DayOfWeek dayOfWeek) {
        return DayOfWeekEnum.valueOf(dayOfWeek.name());
    }

    /**
     * Converts this enum to {@link java.time.DayOfWeek}.
     */
    public java.time.DayOfWeek toDayOfWeek() {
        return java.time.DayOfWeek.valueOf(this.name());
    }
}
