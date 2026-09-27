package com.example.ehrsystem.modules.labtest.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.laborder.repository.LabOrderItemRepository;
import com.example.ehrsystem.modules.labresult.repository.LabResultValueRepository;
import com.example.ehrsystem.modules.labtest.dto.request.CreateLabTestRequest;
import com.example.ehrsystem.modules.labtest.dto.request.UpdateLabTestRequest;
import com.example.ehrsystem.modules.labtest.dto.response.LabTestResponse;
import com.example.ehrsystem.modules.labtest.dto.response.LabTestSummaryResponse;
import com.example.ehrsystem.modules.labtest.entity.LabTest;
import com.example.ehrsystem.modules.labtest.repository.LabTestRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lab test catalog administration. The catalog is patient/encounter
 * independent reference data; codes are unique within their code system
 * and tests referenced by clinical records are deactivated, not deleted.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabTestService {

    private final LabTestRepository labTestRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final LabResultValueRepository labResultValueRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public LabTestResponse create(CreateLabTestRequest request) {
        String code = request.getCode().trim();
        String codeSystem = normalize(request.getCodeSystem());

        assertReferenceRange(request.getDefaultReferenceLow(), request.getDefaultReferenceHigh());

        boolean duplicate = codeSystem != null
                ? labTestRepository.existsByCodeAndCodeSystemAndDeletedAtIsNull(code, codeSystem)
                : labTestRepository.existsByCodeAndCodeSystemIsNullAndDeletedAtIsNull(code);
        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A lab test with this code already exists in the catalog.");
        }

        Long currentUserId = securityContext.getCurrentUserId();

        LabTest labTest = LabTest.builder()
                .code(code)
                .codeSystem(codeSystem)
                .name(request.getName().trim())
                .shortName(normalize(request.getShortName()))
                .description(normalize(request.getDescription()))
                .category(normalize(request.getCategory()))
                .specimenType(request.getSpecimenType())
                .resultType(request.getResultType())
                .unit(normalize(request.getUnit()))
                .defaultReferenceLow(request.getDefaultReferenceLow())
                .defaultReferenceHigh(request.getDefaultReferenceHigh())
                .defaultReferenceText(normalize(request.getDefaultReferenceText()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        LabTest saved = labTestRepository.save(labTest);

        auditLogger.logCustomEvent("LAB_TEST_CREATED", Map.of(
                "code", saved.getCode(),
                "resultType", saved.getResultType().name(),
                "active", saved.getIsActive()
        ));

        return toResponse(saved);
    }

    public LabTestResponse get(UUID labTestUuid) {
        return toResponse(requireLabTest(labTestUuid));
    }

    public Page<LabTestSummaryResponse> search(String query, String code, String category,
                                               Boolean active, Pageable pageable) {
        String term = normalize(query);
        String codeTerm = normalize(code);
        String categoryTerm = normalize(category);
        Specification<LabTest> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (term != null) {
                String pattern = "%" + term.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("shortName")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)));
            }
            if (codeTerm != null) {
                predicates.add(cb.like(cb.lower(root.get("code")),
                        "%" + codeTerm.toLowerCase() + "%"));
            }
            if (categoryTerm != null) {
                predicates.add(cb.like(cb.lower(root.get("category")),
                        "%" + categoryTerm.toLowerCase() + "%"));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return labTestRepository.findAll(spec, pageable).map(this::toSummary);
    }

    @Transactional
    public LabTestResponse update(UUID labTestUuid, UpdateLabTestRequest request) {
        LabTest labTest = requireLabTest(labTestUuid);
        assertVersion(labTest, request.getVersion());

        assertReferenceRange(request.getDefaultReferenceLow(), request.getDefaultReferenceHigh());

        if (request.getName() != null && !request.getName().isBlank()) {
            labTest.setName(request.getName().trim());
        }
        if (request.getShortName() != null) {
            labTest.setShortName(normalize(request.getShortName()));
        }
        if (request.getDescription() != null) {
            labTest.setDescription(normalize(request.getDescription()));
        }
        if (request.getCategory() != null) {
            labTest.setCategory(normalize(request.getCategory()));
        }
        if (request.getSpecimenType() != null) {
            labTest.setSpecimenType(request.getSpecimenType());
        }
        if (request.getResultType() != null && request.getResultType() != labTest.getResultType()) {
            if (labResultValueRepository.countByLabTestIdAndDeletedAtIsNull(labTest.getId()) > 0) {
                throw new IllegalArgumentException(
                        "Cannot change the result type of a lab test that has recorded results");
            }
            labTest.setResultType(request.getResultType());
        }
        if (request.getUnit() != null) {
            labTest.setUnit(normalize(request.getUnit()));
        }
        if (request.getDefaultReferenceLow() != null) {
            labTest.setDefaultReferenceLow(request.getDefaultReferenceLow());
        }
        if (request.getDefaultReferenceHigh() != null) {
            labTest.setDefaultReferenceHigh(request.getDefaultReferenceHigh());
        }
        if (request.getDefaultReferenceText() != null) {
            labTest.setDefaultReferenceText(normalize(request.getDefaultReferenceText()));
        }
        boolean deactivating = false;
        if (request.getIsActive() != null) {
            deactivating = Boolean.FALSE.equals(request.getIsActive())
                    && !Boolean.FALSE.equals(labTest.getIsActive());
            labTest.setIsActive(request.getIsActive());
        }
        labTest.setUpdatedBy(securityContext.getCurrentUserId());

        LabTest saved = labTestRepository.save(labTest);

        auditLogger.logCustomEvent(deactivating ? "LAB_TEST_DEACTIVATED" : "LAB_TEST_UPDATED",
                Map.of(
                        "code", saved.getCode(),
                        "active", saved.getIsActive(),
                        "version", saved.getVersion()
                ));

        return toResponse(saved);
    }

    /**
     * Soft delete. Historical clinical records always keep their reference,
     * so a referenced test can only be deactivated instead.
     */
    @Transactional
    public void delete(UUID labTestUuid) {
        LabTest labTest = requireLabTest(labTestUuid);

        boolean referencedByOrders =
                labOrderItemRepository.countByLabTestIdAndDeletedAtIsNull(labTest.getId()) > 0;
        boolean referencedByResults =
                labResultValueRepository.countByLabTestIdAndDeletedAtIsNull(labTest.getId()) > 0;
        if (referencedByOrders || referencedByResults) {
            throw new IllegalArgumentException(
                    "Cannot delete a lab test that is referenced by clinical records. Set it inactive instead.");
        }

        labTest.setDeletedAt(LocalDateTime.now());
        labTest.setUpdatedBy(securityContext.getCurrentUserId());
        labTestRepository.save(labTest);

        auditLogger.logCustomEvent("LAB_TEST_DELETED", Map.of(
                "code", labTest.getCode(),
                "active", Boolean.FALSE
        ));
    }

    private LabTest requireLabTest(UUID uuid) {
        return labTestRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lab test not found with UUID: " + uuid));
    }

    private void assertReferenceRange(java.math.BigDecimal low, java.math.BigDecimal high) {
        if (low != null && high != null && low.compareTo(high) > 0) {
            throw new IllegalArgumentException(
                    "Default reference low cannot exceed default reference high");
        }
    }

    private void assertVersion(LabTest labTest, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(labTest.getVersion())) {
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

    private LabTestResponse toResponse(LabTest labTest) {
        return LabTestResponse.builder()
                .id(labTest.getId())
                .uuid(labTest.getUuid())
                .code(labTest.getCode())
                .codeSystem(labTest.getCodeSystem())
                .name(labTest.getName())
                .shortName(labTest.getShortName())
                .description(labTest.getDescription())
                .category(labTest.getCategory())
                .specimenType(labTest.getSpecimenType())
                .resultType(labTest.getResultType())
                .unit(labTest.getUnit())
                .defaultReferenceLow(labTest.getDefaultReferenceLow())
                .defaultReferenceHigh(labTest.getDefaultReferenceHigh())
                .defaultReferenceText(labTest.getDefaultReferenceText())
                .isActive(labTest.getIsActive())
                .createdAt(labTest.getCreatedAt())
                .updatedAt(labTest.getUpdatedAt())
                .version(labTest.getVersion())
                .build();
    }

    private LabTestSummaryResponse toSummary(LabTest labTest) {
        return LabTestSummaryResponse.builder()
                .uuid(labTest.getUuid())
                .code(labTest.getCode())
                .name(labTest.getName())
                .shortName(labTest.getShortName())
                .category(labTest.getCategory())
                .specimenType(labTest.getSpecimenType())
                .resultType(labTest.getResultType())
                .unit(labTest.getUnit())
                .defaultReferenceLow(labTest.getDefaultReferenceLow())
                .defaultReferenceHigh(labTest.getDefaultReferenceHigh())
                .defaultReferenceText(labTest.getDefaultReferenceText())
                .isActive(labTest.getIsActive())
                .build();
    }
}
