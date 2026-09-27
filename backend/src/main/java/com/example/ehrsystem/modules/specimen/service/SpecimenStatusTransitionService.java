package com.example.ehrsystem.modules.specimen.service;

import com.example.ehrsystem.modules.specimen.entity.SpecimenStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized specimen state machine.
 *
 * PENDING_COLLECTION -> COLLECTED, CANCELLED
 * COLLECTED          -> RECEIVED, REJECTED, CANCELLED
 * RECEIVED           -> (terminal)
 * REJECTED           -> (terminal)
 * CANCELLED          -> (terminal)
 */
@Component
public class SpecimenStatusTransitionService {

    public void validate(SpecimenStatus currentStatus, SpecimenStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == SpecimenStatus.RECEIVED
                || currentStatus == SpecimenStatus.REJECTED
                || currentStatus == SpecimenStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change specimen status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case PENDING_COLLECTION:
                allowed = (newStatus == SpecimenStatus.COLLECTED
                        || newStatus == SpecimenStatus.CANCELLED);
                break;
            case COLLECTED:
                allowed = (newStatus == SpecimenStatus.RECEIVED
                        || newStatus == SpecimenStatus.REJECTED
                        || newStatus == SpecimenStatus.CANCELLED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid specimen status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
