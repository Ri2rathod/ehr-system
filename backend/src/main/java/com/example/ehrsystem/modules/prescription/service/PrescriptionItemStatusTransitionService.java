package com.example.ehrsystem.modules.prescription.service;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionItemStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized prescription item state machine.
 *
 * ACTIVE        -> COMPLETED, CANCELLED, DISCONTINUED
 * COMPLETED     -> (terminal)
 * CANCELLED     -> (terminal)
 * DISCONTINUED  -> (terminal)
 *
 * Item status is independent of the prescription status: discontinuing
 * one medication never cancels the whole prescription.
 */
@Component
public class PrescriptionItemStatusTransitionService {

    public void validate(PrescriptionItemStatus currentStatus, PrescriptionItemStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == PrescriptionItemStatus.COMPLETED
                || currentStatus == PrescriptionItemStatus.CANCELLED
                || currentStatus == PrescriptionItemStatus.DISCONTINUED) {
            throw new IllegalArgumentException(
                    "Cannot change prescription item status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case ACTIVE:
                allowed = (newStatus == PrescriptionItemStatus.COMPLETED
                        || newStatus == PrescriptionItemStatus.CANCELLED
                        || newStatus == PrescriptionItemStatus.DISCONTINUED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid prescription item status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
