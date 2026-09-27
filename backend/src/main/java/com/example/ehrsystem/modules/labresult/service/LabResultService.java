package com.example.ehrsystem.modules.labresult.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import com.example.ehrsystem.modules.laborder.entity.LabOrderItem;
import com.example.ehrsystem.modules.laborder.entity.LabOrderStatus;
import com.example.ehrsystem.modules.laborder.repository.LabOrderItemRepository;
import com.example.ehrsystem.modules.laborder.repository.LabOrderRepository;
import com.example.ehrsystem.modules.labresult.dto.request.CreateLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.request.CreateLabResultValueRequest;
import com.example.ehrsystem.modules.labresult.dto.request.CorrectLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.request.UpdateLabResultRequest;
import com.example.ehrsystem.modules.labresult.dto.response.LabResultResponse;
import com.example.ehrsystem.modules.labresult.dto.response.LabResultValueResponse;
import com.example.ehrsystem.modules.labresult.entity.AbnormalFlag;
import com.example.ehrsystem.modules.labresult.entity.LabResult;
import com.example.ehrsystem.modules.labresult.entity.LabResultStatus;
import com.example.ehrsystem.modules.labresult.entity.LabResultValue;
import com.example.ehrsystem.modules.labresult.repository.LabResultRepository;
import com.example.ehrsystem.modules.labresult.repository.LabResultValueRepository;
import com.example.ehrsystem.modules.labtest.entity.LabTest;
import com.example.ehrsystem.modules.labtest.entity.ResultType;
import com.example.ehrsystem.modules.labtest.repository.LabTestRepository;
import com.example.ehrsystem.modules.specimen.entity.Specimen;
import com.example.ehrsystem.modules.specimen.entity.SpecimenStatus;
import com.example.ehrsystem.modules.specimen.repository.SpecimenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lab results: PRELIMINARY -> FINAL -> CORRECTED, PRELIMINARY -> CANCELLED.
 * Results are entered against a lab order item and a RECEIVED specimen of
 * the same order. Validation enforces one value representation per result
 * type, copies catalog reference ranges at result time and infers numeric
 * abnormal flags only when an explicit flag is not provided.
 * Specimen handling and results are independent of the encounter status.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabResultValueRepository labResultValueRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final SpecimenRepository specimenRepository;
    private final LabTestRepository labTestRepository;
    private final LabResultStatusTransitionService transitionService;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public LabResultResponse create(UUID labOrderUuid, CreateLabResultRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        assertOrderAcceptsResults(order);
        LabOrderItem item = requireItem(order, request.getLabOrderItemUuid());
        Specimen specimen = requireSpecimen(order, request.getSpecimenUuid());
        if (specimen.getStatus() != SpecimenStatus.RECEIVED) {
            throw new IllegalArgumentException(
                    "Specimen must be received before entering results. Current status: "
                            + specimen.getStatus());
        }

        Long currentUserId = securityContext.getCurrentUserId();

        LabResult result = LabResult.builder()
                .labOrderItem(item)
                .specimen(specimen)
                .resultedAt(LocalDateTime.now())
                .comments(normalize(request.getComments()))
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        LabResult saved = labResultRepository.save(result);
        saveValues(saved, request.getValues(), currentUserId);
        labResultRepository.save(saved);

        auditLogger.logCustomEvent("LAB_RESULT_CREATED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name(),
                "testCode", item.getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    public List<LabResultResponse> list(UUID labOrderUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        return labResultRepository.findByLabOrderIdAndDeletedAtIsNull(order.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LabResultResponse get(UUID labOrderUuid, UUID resultUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        return toResponse(requireResult(order, resultUuid));
    }

    @Transactional
    public LabResultResponse update(UUID labOrderUuid, UUID resultUuid,
                                    UpdateLabResultRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        LabResult result = requireResult(order, resultUuid);
        assertEditable(result);
        assertVersion(result, request.getVersion());

        Long currentUserId = securityContext.getCurrentUserId();

        if (request.getComments() != null) {
            result.setComments(normalize(request.getComments()));
        }
        result.setUpdatedBy(currentUserId);

        replaceValues(result, request.getValues(), currentUserId);
        LabResult saved = labResultRepository.save(result);

        auditLogger.logCustomEvent("LAB_RESULT_UPDATED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name(),
                "testCode", saved.getLabOrderItem().getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    @Transactional
    public LabResultResponse finalizeResult(UUID labOrderUuid, UUID resultUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        LabResult result = requireResult(order, resultUuid);

        if (result.getStatus() == LabResultStatus.FINAL) {
            throw new IllegalArgumentException("Lab result is already finalized");
        }
        transitionService.validate(result.getStatus(), LabResultStatus.FINAL);

        Long currentUserId = securityContext.getCurrentUserId();
        result.setStatus(LabResultStatus.FINAL);
        result.setVerifiedAt(LocalDateTime.now());
        result.setVerifiedBy(currentUserId);
        result.setUpdatedBy(currentUserId);

        LabResult saved = labResultRepository.save(result);

        auditLogger.logCustomEvent("LAB_RESULT_FINALIZED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name(),
                "testCode", saved.getLabOrderItem().getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    @Transactional
    public LabResultResponse correct(UUID labOrderUuid, UUID resultUuid,
                                     CorrectLabResultRequest request) {
        LabOrder order = requireOrder(labOrderUuid);
        LabResult result = requireResult(order, resultUuid);

        if (result.getStatus() == LabResultStatus.CORRECTED) {
            throw new IllegalArgumentException("Lab result has already been corrected");
        }
        transitionService.validate(result.getStatus(), LabResultStatus.CORRECTED);
        assertVersion(result, request.getVersion());

        Long currentUserId = securityContext.getCurrentUserId();

        result.setStatus(LabResultStatus.CORRECTED);
        result.setCorrectionReason(request.getCorrectionReason().trim());
        if (request.getComments() != null) {
            result.setComments(normalize(request.getComments()));
        }
        result.setVerifiedAt(LocalDateTime.now());
        result.setVerifiedBy(currentUserId);
        result.setUpdatedBy(currentUserId);

        replaceValues(result, request.getValues(), currentUserId);
        LabResult saved = labResultRepository.save(result);

        auditLogger.logCustomEvent("LAB_RESULT_CORRECTED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name(),
                "testCode", saved.getLabOrderItem().getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    @Transactional
    public LabResultResponse cancel(UUID labOrderUuid, UUID resultUuid) {
        LabOrder order = requireOrder(labOrderUuid);
        LabResult result = requireResult(order, resultUuid);

        if (result.getStatus() == LabResultStatus.CANCELLED) {
            throw new IllegalArgumentException("Lab result is already cancelled");
        }
        transitionService.validate(result.getStatus(), LabResultStatus.CANCELLED);

        Long currentUserId = securityContext.getCurrentUserId();
        result.setStatus(LabResultStatus.CANCELLED);
        result.setUpdatedBy(currentUserId);

        LabResult saved = labResultRepository.save(result);

        auditLogger.logCustomEvent("LAB_RESULT_CANCELLED", Map.of(
                "orderNumber", order.getOrderNumber(),
                "status", saved.getStatus().name(),
                "testCode", saved.getLabOrderItem().getLabTest().getCode()
        ));

        return toResponse(saved);
    }

    /** Replaces the value set: old rows are soft deleted for history. */
    private void replaceValues(LabResult result, List<CreateLabResultValueRequest> requests,
                               Long currentUserId) {
        List<LabResultValue> existing =
                labResultValueRepository.findByLabResultIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        result.getId());
        LocalDateTime now = LocalDateTime.now();
        for (LabResultValue value : existing) {
            value.setDeletedAt(now);
            value.setUpdatedBy(currentUserId);
        }
        labResultValueRepository.saveAll(existing);

        saveValues(result, requests, currentUserId);
    }

    private void saveValues(LabResult result, List<CreateLabResultValueRequest> requests,
                            Long currentUserId) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one result value is required");
        }
        for (CreateLabResultValueRequest request : requests) {
            LabTest test = requireLabTest(request.getLabTestUuid());
            validateRepresentation(test, request);

            BigDecimal referenceLow = request.getReferenceLow() != null
                    ? request.getReferenceLow() : test.getDefaultReferenceLow();
            BigDecimal referenceHigh = request.getReferenceHigh() != null
                    ? request.getReferenceHigh() : test.getDefaultReferenceHigh();
            String referenceText = request.getReferenceText() != null
                    ? request.getReferenceText() : test.getDefaultReferenceText();
            String unit = request.getUnit() != null ? request.getUnit() : test.getUnit();
            AbnormalFlag flag = request.getAbnormalFlag() != null
                    ? request.getAbnormalFlag()
                    : inferAbnormalFlag(request.getValueNumeric(), referenceLow, referenceHigh);

            LabResultValue value = LabResultValue.builder()
                    .labResult(result)
                    .labTest(test)
                    .valueNumeric(request.getValueNumeric())
                    .valueText(request.getValueText())
                    .valueCode(request.getValueCode())
                    .unit(unit)
                    .referenceLow(referenceLow)
                    .referenceHigh(referenceHigh)
                    .referenceText(referenceText)
                    .abnormalFlag(flag)
                    .notes(normalize(request.getNotes()))
                    .createdBy(currentUserId)
                    .updatedBy(currentUserId)
                    .build();
            labResultValueRepository.save(value);
        }
    }

    /**
     * Exactly one representation matching the lab test's result type.
     * Numeric low/high inference only runs for an explicit missing flag and
     * only when both reference bounds exist - critical thresholds are never
     * guessed, and an explicit flag is never overwritten.
     */
    private void validateRepresentation(LabTest test, CreateLabResultValueRequest request) {
        String testName = test.getName();
        boolean hasNumeric = request.getValueNumeric() != null;
        boolean hasText = request.getValueText() != null;
        boolean hasCode = request.getValueCode() != null;

        switch (test.getResultType()) {
            case NUMERIC:
                if (!hasNumeric) {
                    throw new IllegalArgumentException(
                            "Value for " + testName + " must be numeric");
                }
                if (hasText || hasCode) {
                    throw new IllegalArgumentException(
                            "Numeric result for " + testName + " cannot also include text or code");
                }
                break;
            case TEXT:
                if (hasNumeric || hasCode) {
                    throw new IllegalArgumentException(
                            "Text result for " + testName + " cannot also include a number or code");
                }
                if (!hasText) {
                    throw new IllegalArgumentException(
                            "Value for " + testName + " must be text");
                }
                break;
            case QUALITATIVE:
                if (hasNumeric) {
                    throw new IllegalArgumentException(
                            "Qualitative result for " + testName + " cannot be numeric");
                }
                if (!hasText && !hasCode) {
                    throw new IllegalArgumentException(
                            "Value for " + testName + " must be text or a code");
                }
                if (hasText && hasCode) {
                    throw new IllegalArgumentException(
                            "Qualitative result for " + testName + " cannot include both text and code");
                }
                break;
            case CODED:
                if (!hasCode) {
                    throw new IllegalArgumentException(
                            "Value for " + testName + " must be a coded value");
                }
                if (hasNumeric || hasText) {
                    throw new IllegalArgumentException(
                            "Coded result for " + testName + " cannot also include a number or text");
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported result type for " + testName + ": " + test.getResultType());
        }
    }

    private AbnormalFlag inferAbnormalFlag(BigDecimal value, BigDecimal low, BigDecimal high) {
        if (value == null || low == null || high == null) {
            return null;
        }
        if (value.compareTo(low) < 0) {
            return AbnormalFlag.LOW;
        }
        if (value.compareTo(high) > 0) {
            return AbnormalFlag.HIGH;
        }
        return AbnormalFlag.NORMAL;
    }

    private void assertEditable(LabResult result) {
        switch (result.getStatus()) {
            case FINAL:
                throw new IllegalArgumentException(
                        "Lab result is already finalized. Use correction to record changes.");
            case CORRECTED:
                throw new IllegalArgumentException("Cannot edit a corrected lab result");
            case CANCELLED:
                throw new IllegalArgumentException("Cannot edit a cancelled lab result");
            default:
                break;
        }
    }

    private LabOrder requireOrder(UUID uuid) {
        return labOrderRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab order not found with UUID: " + uuid));
    }

    private LabResult requireResult(LabOrder order, UUID resultUuid) {
        return labResultRepository
                .findByUuidAndLabOrderIdAndDeletedAtIsNull(resultUuid, order.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab result not found with UUID: " + resultUuid));
    }

    private LabOrderItem requireItem(LabOrder order, UUID itemUuid) {
        return labOrderItemRepository
                .findByUuidAndLabOrderIdAndDeletedAtIsNull(itemUuid, order.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab order item not found with UUID: " + itemUuid));
    }

    private Specimen requireSpecimen(LabOrder order, UUID specimenUuid) {
        return specimenRepository
                .findByUuidAndLabOrderIdAndDeletedAtIsNull(specimenUuid, order.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Specimen not found with UUID: " + specimenUuid));
    }

    private LabTest requireLabTest(UUID labTestUuid) {
        return labTestRepository.findByUuidAndDeletedAtIsNull(labTestUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab test not found with UUID: " + labTestUuid));
    }

    private void assertOrderAcceptsResults(LabOrder order) {
        if (order.getStatus() == LabOrderStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot add results to a draft lab order");
        }
        if (order.getStatus() == LabOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot add results to a cancelled lab order");
        }
    }

    private void assertVersion(LabResult result, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(result.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This clinical record was updated by another user. Reload to continue.");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LabResultResponse toResponse(LabResult result) {
        List<LabResultValueResponse> values = labResultValueRepository
                .findByLabResultIdAndDeletedAtIsNullOrderByCreatedAtAsc(result.getId())
                .stream()
                .map(this::toValueResponse)
                .toList();

        return LabResultResponse.builder()
                .id(result.getId())
                .uuid(result.getUuid())
                .labOrderUuid(result.getLabOrderItem().getLabOrder().getUuid())
                .orderNumber(result.getLabOrderItem().getLabOrder().getOrderNumber())
                .labOrderItemUuid(result.getLabOrderItem().getUuid())
                .labTestCode(result.getLabOrderItem().getLabTest().getCode())
                .labTestName(result.getLabOrderItem().getLabTest().getName())
                .specimenUuid(result.getSpecimen().getUuid())
                .specimenType(result.getSpecimen().getSpecimenType().name())
                .specimenStatus(result.getSpecimen().getStatus().name())
                .collectedAt(result.getSpecimen().getCollectedAt())
                .receivedAt(result.getSpecimen().getReceivedAt())
                .status(result.getStatus())
                .resultedAt(result.getResultedAt())
                .verifiedAt(result.getVerifiedAt())
                .verifiedBy(result.getVerifiedBy())
                .comments(result.getComments())
                .correctionReason(result.getCorrectionReason())
                .values(values)
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .version(result.getVersion())
                .build();
    }

    private LabResultValueResponse toValueResponse(LabResultValue value) {
        return LabResultValueResponse.builder()
                .id(value.getId())
                .uuid(value.getUuid())
                .labTestUuid(value.getLabTest().getUuid())
                .labTestCode(value.getLabTest().getCode())
                .labTestName(value.getLabTest().getName())
                .valueNumeric(value.getValueNumeric())
                .valueText(value.getValueText())
                .valueCode(value.getValueCode())
                .unit(value.getUnit())
                .referenceLow(value.getReferenceLow())
                .referenceHigh(value.getReferenceHigh())
                .referenceText(value.getReferenceText())
                .abnormalFlag(value.getAbnormalFlag())
                .notes(value.getNotes())
                .version(value.getVersion())
                .build();
    }
}
