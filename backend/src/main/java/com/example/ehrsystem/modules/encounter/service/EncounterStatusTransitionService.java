package com.example.ehrsystem.modules.encounter.service;

import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized encounter status state machine.
 *
 * DRAFT        -> IN_PROGRESS, CANCELLED
 * IN_PROGRESS  -> COMPLETED, CANCELLED
 * COMPLETED    -> (terminal)
 * CANCELLED    -> (terminal)
 */
@Component
public class EncounterStatusTransitionService {

    public void validate(EncounterStatus currentStatus, EncounterStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == EncounterStatus.COMPLETED || currentStatus == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot change status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case DRAFT:
                allowed = (newStatus == EncounterStatus.IN_PROGRESS ||
                           newStatus == EncounterStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                allowed = (newStatus == EncounterStatus.COMPLETED ||
                           newStatus == EncounterStatus.CANCELLED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid encounter status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
