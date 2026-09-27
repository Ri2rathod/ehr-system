package com.example.ehrsystem.modules.medication.entity;

/**
 * Units shared by medication strength and prescription dose.
 * One controlled unit concept - no duplicate enums across modules.
 */
public enum DoseUnit {
    MG,
    MCG,
    G,
    ML,
    IU,
    MEQ,
    PERCENT,
    PUFF,
    DROP,
    UNIT,
    OTHER
}
