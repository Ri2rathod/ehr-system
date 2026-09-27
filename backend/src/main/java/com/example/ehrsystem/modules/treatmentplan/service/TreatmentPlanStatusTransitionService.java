package com.example.ehrsystem.modules.treatmentplan.service;

import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentItemStatus;
import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized treatment plan / item state machines.
 *
 * Plan:   DRAFT     -> ACTIVE, CANCELLED
 *         ACTIVE    -> COMPLETED, CANCELLED
 *         COMPLETED -> (terminal)
 *         CANCELLED -> (terminal)
 *
 * Item:   PLANNED     -> IN_PROGRESS, CANCELLED
 *         IN_PROGRESS -> COMPLETED, CANCELLED
 *         COMPLETED   -> (terminal)
 *         CANCELLED   -> (terminal)
 */
@Component
public class TreatmentPlanStatusTransitionService {

    public void validatePlan(TreatmentPlanStatus currentStatus, TreatmentPlanStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == TreatmentPlanStatus.COMPLETED
                || currentStatus == TreatmentPlanStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change treatment plan status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case DRAFT:
                allowed = (newStatus == TreatmentPlanStatus.ACTIVE
                        || newStatus == TreatmentPlanStatus.CANCELLED);
                break;
            case ACTIVE:
                allowed = (newStatus == TreatmentPlanStatus.COMPLETED
                        || newStatus == TreatmentPlanStatus.CANCELLED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid treatment plan status transition from " + currentStatus + " to " + newStatus);
        }
    }

    public void validateItem(TreatmentItemStatus currentStatus, TreatmentItemStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == TreatmentItemStatus.COMPLETED
                || currentStatus == TreatmentItemStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change treatment item status from terminal state: " + currentStatus);
        }

        boolean allowed = false;
        switch (currentStatus) {
            case PLANNED:
                allowed = (newStatus == TreatmentItemStatus.IN_PROGRESS
                        || newStatus == TreatmentItemStatus.CANCELLED);
                break;
            case IN_PROGRESS:
                allowed = (newStatus == TreatmentItemStatus.COMPLETED
                        || newStatus == TreatmentItemStatus.CANCELLED);
                break;
            default:
                allowed = false;
        }

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Invalid treatment item status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
