package com.example.ehrsystem.modules.labresult.service;

import com.example.ehrsystem.modules.labresult.entity.LabResultStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized lab result state machine.
 *
 * PRELIMINARY -> FINAL, CANCELLED
 * FINAL       -> CORRECTED
 * CORRECTED   -> (terminal)
 * CANCELLED   -> (terminal)
 */
@Component
public class LabResultStatusTransitionService {

    public void validate(LabResultStatus currentStatus, LabResultStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == LabResultStatus.CORRECTED
                || currentStatus == LabResultStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change lab result status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case PRELIMINARY:
                allowed = (newStatus == LabResultStatus.FINAL
                        || newStatus == LabResultStatus.CANCELLED);
                break;
            case FINAL:
                allowed = (newStatus == LabResultStatus.CORRECTED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid lab result status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
