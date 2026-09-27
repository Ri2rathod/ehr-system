package com.example.ehrsystem.modules.prescription.service;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized prescription state machine.
 *
 * DRAFT     -> ACTIVE, CANCELLED
 * ACTIVE    -> COMPLETED, CANCELLED, VOID
 * COMPLETED -> (terminal)
 * CANCELLED -> (terminal)
 * VOID      -> (terminal)
 *
 * VOID is only reachable from ACTIVE (an already-issued order is
 * invalidated); CANCELLED is the pre-activation workflow cancellation.
 */
@Component
public class PrescriptionStatusTransitionService {

    public void validate(PrescriptionStatus currentStatus, PrescriptionStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == PrescriptionStatus.COMPLETED
                || currentStatus == PrescriptionStatus.CANCELLED
                || currentStatus == PrescriptionStatus.VOID) {
            throw new IllegalArgumentException(
                    "Cannot change prescription status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case DRAFT:
                allowed = (newStatus == PrescriptionStatus.ACTIVE
                        || newStatus == PrescriptionStatus.CANCELLED);
                break;
            case ACTIVE:
                allowed = (newStatus == PrescriptionStatus.COMPLETED
                        || newStatus == PrescriptionStatus.CANCELLED
                        || newStatus == PrescriptionStatus.VOID);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid prescription status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
