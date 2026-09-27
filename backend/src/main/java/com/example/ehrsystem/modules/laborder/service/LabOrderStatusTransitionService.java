package com.example.ehrsystem.modules.laborder.service;

import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized lab order state machine.
 *
 * DRAFT      -> ORDERED, CANCELLED
 * ORDERED    -> IN_PROGRESS, COMPLETED, CANCELLED
 * IN_PROGRESS-> COMPLETED, CANCELLED
 * COMPLETED  -> (terminal)
 * CANCELLED  -> (terminal)
 */
@Component
public class LabOrderStatusTransitionService {

    public void validate(LabOrderStatus currentStatus, LabOrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == LabOrderStatus.COMPLETED
                || currentStatus == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change lab order status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case DRAFT:
                allowed = (newStatus == LabOrderStatus.ORDERED
                        || newStatus == LabOrderStatus.CANCELLED);
                break;
            case ORDERED:
                allowed = (newStatus == LabOrderStatus.IN_PROGRESS
                        || newStatus == LabOrderStatus.COMPLETED
                        || newStatus == LabOrderStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                allowed = (newStatus == LabOrderStatus.COMPLETED
                        || newStatus == LabOrderStatus.CANCELLED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid lab order status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
