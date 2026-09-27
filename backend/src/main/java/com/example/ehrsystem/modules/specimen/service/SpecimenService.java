package com.example.ehrsystem.modules.specimen.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import com.example.ehrsystem.modules.laborder.repository.LabOrderRepository;
import com.example.ehrsystem.modules.specimen.dto.request.CollectSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.CreateSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.ReceiveSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.request.RejectSpecimenRequest;
import com.example.ehrsystem.modules.specimen.dto.response.SpecimenResponse;
import com.example.ehrsystem.modules.specimen.entity.Specimen;
import com.example.ehrsystem.modules.specimen.entity.SpecimenStatus;
import com.example.ehrsystem.modules.specimen.repository.SpecimenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Specimen lifecycle for a lab order. Specimens are processed by the lab
 * independently of the encounter status - only the parent order gates
 * specimen creation. All transitions go through the transition service;
 * collection and receipt operations are idempotent.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecimenService {

    private final SpecimenRepository specimenRepository;
    private final LabOrderRepository labOrderRepository;
    private final SpecimenStatusTransitionService transitionService;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public SpecimenResponse create(UUID labOrderUuid, CreateSpecimenRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        assertOrderAcceptsSpecimen(order);

        Long currentUserId = securityContext.getCurrentUserId();

        Specimen specimen = Specimen.builder()
                .labOrder(order)
                .specimenType(request.getSpecimenType())
                .specimenIdentifier(normalize(request.getSpecimenIdentifier()))
                .notes(normalize(request.getNotes()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        Specimen saved = specimenRepository.save(specimen);

        auditLogger.logCustomEvent("SPECIMEN_CREATED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "specimenType", saved.getSpecimenType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    public List<SpecimenResponse> list(UUID labOrderUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        return specimenRepository.findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SpecimenResponse get(UUID labOrderUuid, UUID specimenUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        return toResponse(requireSpecimen(order, specimenUuid));
    }

    @Transactional
    public SpecimenResponse collect(UUID labOrderUuid, UUID specimenUuid,
                                    CollectSpecimenRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        Specimen specimen = requireSpecimen(order, specimenUuid);

        if (specimen.getStatus() == SpecimenStatus.COLLECTED) {
            return toResponse(specimen);
        }
        transitionService.validate(specimen.getStatus(), SpecimenStatus.COLLECTED);

        specimen.setStatus(SpecimenStatus.COLLECTED);
        specimen.setCollectedAt(request.getCollectedAt() != null
                ? request.getCollectedAt() : LocalDateTime.now());
        specimen.setCollectedBy(securityContext.getCurrentUserId());
        if (request.getSpecimenIdentifier() != null) {
            specimen.setSpecimenIdentifier(normalize(request.getSpecimenIdentifier()));
        }
        specimen.setUpdatedBy(securityContext.getCurrentUserId());

        Specimen saved = specimenRepository.save(specimen);

        auditLogger.logCustomEvent("SPECIMEN_COLLECTED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "specimenType", saved.getSpecimenType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public SpecimenResponse receive(UUID labOrderUuid, UUID specimenUuid,
                                    ReceiveSpecimenRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        Specimen specimen = requireSpecimen(order, specimenUuid);

        if (specimen.getStatus() == SpecimenStatus.RECEIVED) {
            return toResponse(specimen);
        }
        transitionService.validate(specimen.getStatus(), SpecimenStatus.RECEIVED);

        specimen.setStatus(SpecimenStatus.RECEIVED);
        specimen.setReceivedAt(request.getReceivedAt() != null
                ? request.getReceivedAt() : LocalDateTime.now());
        specimen.setReceivedBy(securityContext.getCurrentUserId());
        specimen.setUpdatedBy(securityContext.getCurrentUserId());

        Specimen saved = specimenRepository.save(specimen);

        auditLogger.logCustomEvent("SPECIMEN_RECEIVED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "specimenType", saved.getSpecimenType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public SpecimenResponse reject(UUID labOrderUuid, UUID specimenUuid,
                                   RejectSpecimenRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        Specimen specimen = requireSpecimen(order, specimenUuid);

        if (specimen.getStatus() == SpecimenStatus.REJECTED) {
            return toResponse(specimen);
        }
        transitionService.validate(specimen.getStatus(), SpecimenStatus.REJECTED);

        specimen.setStatus(SpecimenStatus.REJECTED);
        specimen.setRejectionReason(request.getRejectionReason().trim());
        if (request.getNotes() != null) {
            specimen.setNotes(normalize(request.getNotes()));
        }
        specimen.setUpdatedBy(securityContext.getCurrentUserId());

        Specimen saved = specimenRepository.save(specimen);

        auditLogger.logCustomEvent("SPECIMEN_REJECTED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "specimenType", saved.getSpecimenType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public SpecimenResponse cancel(UUID labOrderUuid, UUID specimenUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        Specimen specimen = requireSpecimen(order, specimenUuid);

        if (specimen.getStatus() == SpecimenStatus.CANCELLED) {
            return toResponse(specimen);
        }
        transitionService.validate(specimen.getStatus(), SpecimenStatus.CANCELLED);

        specimen.setStatus(SpecimenStatus.CANCELLED);
        specimen.setUpdatedBy(securityContext.getCurrentUserId());

        Specimen saved = specimenRepository.save(specimen);

        auditLogger.logCustomEvent("SPECIMEN_CANCELLED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "specimenType", saved.getSpecimenType().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    private LabOrder requireOrder(UUID uuid) {
        return labOrderRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab order not found with UUID: " + uuid));
    }

    private Specimen requireSpecimen(LabOrder order, UUID specimenUuid) {
        return specimenRepository.findByUuidAndLabOrderIdAndDeletedAtIsNull(specimenUuid, order.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Specimen not found with UUID: " + specimenUuid));
    }

    private void assertOrderAcceptsSpecimen(LabOrder order) {
        if (order.getStatus() == LabOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot add specimens to a draft lab order");
        }
        if (order.getStatus() == LabOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot add specimens to a completed lab order");
        }
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot add specimens to a cancelled lab order");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SpecimenResponse toResponse(Specimen specimen) {
        return SpecimenResponse.builder()
                .id(specimen.getId())
                .uuid(specimen.getUuid())
                .labOrderUuid(specimen.getLabOrder().getUuid())
                .orderNumber(specimen.getLabOrder().getOrderNumber())
                .specimenType(specimen.getSpecimenType())
                .specimenIdentifier(specimen.getSpecimenIdentifier())
                .status(specimen.getStatus())
                .collectedAt(specimen.getCollectedAt())
                .collectedBy(specimen.getCollectedBy())
                .receivedAt(specimen.getReceivedAt())
                .receivedBy(specimen.getReceivedBy())
                .rejectionReason(specimen.getRejectionReason())
                .notes(specimen.getNotes())
                .createdAt(specimen.getCreatedAt())
                .updatedAt(specimen.getUpdatedAt())
                .version(specimen.getVersion())
                .build();
    }
}
