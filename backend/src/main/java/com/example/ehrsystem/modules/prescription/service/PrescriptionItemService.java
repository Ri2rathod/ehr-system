package com.example.ehrsystem.modules.prescription.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.medication.entity.Medication;
import com.example.ehrsystem.modules.medication.repository.MedicationRepository;
import com.example.ehrsystem.modules.prescription.dto.request.CreatePrescriptionItemRequest;
import com.example.ehrsystem.modules.prescription.dto.request.UpdatePrescriptionItemRequest;
import com.example.ehrsystem.modules.prescription.dto.response.PrescriptionItemResponse;
import com.example.ehrsystem.modules.prescription.entity.FrequencyUnit;
import com.example.ehrsystem.modules.prescription.entity.MedicationFrequency;
import com.example.ehrsystem.modules.prescription.entity.Prescription;
import com.example.ehrsystem.modules.prescription.entity.PrescriptionItem;
import com.example.ehrsystem.modules.prescription.entity.PrescriptionItemStatus;
import com.example.ehrsystem.modules.prescription.entity.PrescriptionStatus;
import com.example.ehrsystem.modules.prescription.entity.QuantityUnit;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionItemRepository;
import com.example.ehrsystem.modules.prescription.repository.PrescriptionRepository;
import com.example.ehrsystem.modules.treatmentplan.entity.DurationUnit;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prescription items are a child domain of Prescription (itself a child of
 * Encounter). The medication identity is immutable for the lifetime of an
 * item: to change the medication, discontinue the item and create a new one.
 * Item status is independent of prescription status - discontinuing one
 * medication never cancels the whole prescription.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionItemService {

    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final EncounterRepository encounterRepository;
    private final MedicationRepository medicationRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;
    private final PrescriptionItemStatusTransitionService transitionService;

    @Transactional
    public PrescriptionItemResponse create(UUID encounterUuid, UUID prescriptionUuid,
                                           CreatePrescriptionItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        assertEncounterModifiable(encounter);
        assertPrescriptionOpen(prescription);

        Medication medication = resolveMedication(request.getMedicationUuid());

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        validateDates(startDate, endDate);
        validateDurationPair(request.getDuration(), request.getDurationUnit());
        validateQuantityPair(request.getQuantity(), request.getQuantityUnit());
        validateFrequencyPair(request.getFrequency(), request.getFrequencyValue(),
                request.getFrequencyUnit());
        validateCustomFrequencyInstructions(request.getFrequency(),
                normalize(request.getInstructions()));

        Long currentUserId = securityContext.getCurrentUserId();

        PrescriptionItem item = PrescriptionItem.builder()
                .prescription(prescription)
                .medication(medication)
                .dose(request.getDose())
                .doseUnit(request.getDoseUnit())
                .route(request.getRoute())
                .frequency(request.getFrequency())
                .frequencyValue(request.getFrequencyValue())
                .frequencyUnit(request.getFrequencyUnit())
                .duration(request.getDuration())
                .durationUnit(request.getDurationUnit())
                .quantity(request.getQuantity())
                .quantityUnit(request.getQuantityUnit())
                .refills(request.getRefills() != null ? request.getRefills() : 0)
                .instructions(normalize(request.getInstructions()))
                .startDate(startDate)
                .endDate(endDate)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();

        PrescriptionItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent("PRESCRIPTION_ITEM_CREATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "route", saved.getRoute().name(),
                "frequency", saved.getFrequency().name(),
                "doseUnit", saved.getDoseUnit().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    public List<PrescriptionItemResponse> list(UUID encounterUuid, UUID prescriptionUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        return itemRepository
                .findByPrescriptionIdAndDeletedAtIsNullOrderByCreatedAtAsc(prescription.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PrescriptionItemResponse update(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid,
                                           UpdatePrescriptionItemRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        PrescriptionItem item = requireItem(prescription, itemUuid);
        assertEncounterModifiable(encounter);
        assertPrescriptionOpen(prescription);
        assertVersion(item, request.getVersion());

        // Null means "leave unchanged": validate against effective values.
        LocalDate effectiveStart = request.getStartDate() != null
                ? request.getStartDate() : item.getStartDate();
        LocalDate effectiveEnd = request.getEndDate() != null
                ? request.getEndDate() : item.getEndDate();
        validateDates(effectiveStart, effectiveEnd);

        if (request.getDuration() != null || request.getDurationUnit() != null) {
            Integer effectiveDuration = request.getDuration() != null
                    ? request.getDuration() : item.getDuration();
            DurationUnit effectiveUnit = request.getDurationUnit() != null
                    ? request.getDurationUnit() : item.getDurationUnit();
            validateDurationPair(effectiveDuration, effectiveUnit);
            item.setDuration(effectiveDuration);
            item.setDurationUnit(effectiveUnit);
        }

        if (request.getQuantity() != null || request.getQuantityUnit() != null) {
            Integer effectiveQuantity = request.getQuantity() != null
                    ? request.getQuantity() : item.getQuantity();
            var effectiveQuantityUnit = request.getQuantityUnit() != null
                    ? request.getQuantityUnit() : item.getQuantityUnit();
            validateQuantityPair(effectiveQuantity, effectiveQuantityUnit);
            item.setQuantity(effectiveQuantity);
            item.setQuantityUnit(effectiveQuantityUnit);
        }

        var effectiveFrequency = request.getFrequency() != null
                ? request.getFrequency() : item.getFrequency();
        Integer effectiveFrequencyValue = request.getFrequencyValue() != null
                ? request.getFrequencyValue() : item.getFrequencyValue();
        var effectiveFrequencyUnit = request.getFrequencyUnit() != null
                ? request.getFrequencyUnit() : item.getFrequencyUnit();
        if (request.getFrequency() != null || request.getFrequencyValue() != null
                || request.getFrequencyUnit() != null) {
            validateFrequencyPair(effectiveFrequency, effectiveFrequencyValue,
                    effectiveFrequencyUnit);
            item.setFrequency(effectiveFrequency);
            item.setFrequencyValue(effectiveFrequencyValue);
            item.setFrequencyUnit(effectiveFrequencyUnit);
        }

        String effectiveInstructions = request.getInstructions() != null
                ? normalize(request.getInstructions()) : item.getInstructions();
        validateCustomFrequencyInstructions(effectiveFrequency, effectiveInstructions);

        if (request.getDose() != null) {
            item.setDose(request.getDose());
        }
        if (request.getDoseUnit() != null) {
            item.setDoseUnit(request.getDoseUnit());
        }
        if (request.getRoute() != null) {
            item.setRoute(request.getRoute());
        }
        if (request.getRefills() != null) {
            item.setRefills(request.getRefills());
        }
        if (request.getInstructions() != null) {
            item.setInstructions(effectiveInstructions);
        }
        if (request.getStartDate() != null) {
            item.setStartDate(effectiveStart);
        }
        if (request.getEndDate() != null) {
            item.setEndDate(effectiveEnd);
        }

        item.setUpdatedBy(securityContext.getCurrentUserId());
        PrescriptionItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent("PRESCRIPTION_ITEM_UPDATED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "route", saved.getRoute().name(),
                "frequency", saved.getFrequency().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    @Transactional
    public PrescriptionItemResponse complete(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid) {
        PrescriptionItem item = requireItemContext(encounterUuid, prescriptionUuid, itemUuid);

        if (item.getStatus() == PrescriptionItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Prescription item is already completed");
        }
        return applyItemTransition(item, PrescriptionItemStatus.COMPLETED,
                "PRESCRIPTION_ITEM_COMPLETED");
    }

    @Transactional
    public PrescriptionItemResponse cancel(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid) {
        PrescriptionItem item = requireItemContext(encounterUuid, prescriptionUuid, itemUuid);

        if (item.getStatus() == PrescriptionItemStatus.CANCELLED) {
            throw new IllegalArgumentException("Prescription item is already cancelled");
        }
        return applyItemTransition(item, PrescriptionItemStatus.CANCELLED,
                "PRESCRIPTION_ITEM_CANCELLED");
    }

    @Transactional
    public PrescriptionItemResponse discontinue(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid) {
        PrescriptionItem item = requireItemContext(encounterUuid, prescriptionUuid, itemUuid);

        if (item.getStatus() == PrescriptionItemStatus.DISCONTINUED) {
            throw new IllegalArgumentException("Prescription item is already discontinued");
        }
        return applyItemTransition(item, PrescriptionItemStatus.DISCONTINUED,
                "PRESCRIPTION_ITEM_DISCONTINUED");
    }

    @Transactional
    public void delete(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid) {
        PrescriptionItem item = requireItemContext(encounterUuid, prescriptionUuid, itemUuid);

        if (item.getStatus() == PrescriptionItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete a completed prescription item");
        }

        item.setDeletedAt(LocalDateTime.now());
        item.setUpdatedBy(securityContext.getCurrentUserId());
        itemRepository.save(item);

        auditLogger.logCustomEvent("PRESCRIPTION_ITEM_DELETED", Map.of(
                "encounterNumber", item.getPrescription().getEncounter().getEncounterNumber(),
                "route", item.getRoute().name(),
                "status", item.getStatus().name()
        ));
    }

    private PrescriptionItem requireItemContext(UUID encounterUuid, UUID prescriptionUuid, UUID itemUuid) {
        Encounter encounter = requireEncounter(encounterUuid);
        Prescription prescription = requirePrescription(encounter, prescriptionUuid);
        PrescriptionItem item = requireItem(prescription, itemUuid);
        assertEncounterModifiable(encounter);
        assertPrescriptionOpen(prescription);
        return item;
    }

    private PrescriptionItemResponse applyItemTransition(PrescriptionItem item,
                                                         PrescriptionItemStatus target,
                                                         String auditEvent) {
        transitionService.validate(item.getStatus(), target);
        item.setStatus(target);
        item.setUpdatedBy(securityContext.getCurrentUserId());

        PrescriptionItem saved = itemRepository.save(item);

        auditLogger.logCustomEvent(auditEvent, Map.of(
                "encounterNumber", saved.getPrescription().getEncounter().getEncounterNumber(),
                "route", saved.getRoute().name(),
                "status", saved.getStatus().name()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private Prescription requirePrescription(Encounter encounter, UUID prescriptionUuid) {
        return prescriptionRepository
                .findByUuidAndEncounterIdAndDeletedAtIsNull(prescriptionUuid, encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Prescription not found with UUID: " + prescriptionUuid));
    }

    private PrescriptionItem requireItem(Prescription prescription, UUID itemUuid) {
        return itemRepository
                .findByUuidAndPrescriptionIdAndDeletedAtIsNull(itemUuid, prescription.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Prescription item not found with UUID: " + itemUuid));
    }

    /** Unknown or soft-deleted medication -> 404; inactive -> 400. */
    private Medication resolveMedication(UUID medicationUuid) {
        Medication medication = medicationRepository.findByUuidAndDeletedAtIsNull(medicationUuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Medication not found with UUID: " + medicationUuid));
        if (!Boolean.TRUE.equals(medication.getIsActive())) {
            throw new IllegalArgumentException(
                    "Medication is not active for prescribing: " + medication.getGenericName());
        }
        return medication;
    }

    private void assertEncounterModifiable(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify prescription items for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify prescription items for a cancelled encounter");
        }
    }

    private void assertPrescriptionOpen(Prescription prescription) {
        if (prescription.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify items of a completed prescription");
        }
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify items of a cancelled prescription");
        }
        if (prescription.getStatus() == PrescriptionStatus.VOID) {
            throw new IllegalArgumentException("Cannot modify items of a voided prescription");
        }
    }

    private void assertVersion(PrescriptionItem item, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(item.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This clinical record was updated by another user. Reload to continue.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    private void validateDurationPair(Integer duration, DurationUnit unit) {
        if (duration != null && unit == null) {
            throw new IllegalArgumentException("Duration unit is required when duration is provided");
        }
        if (unit != null && duration == null) {
            throw new IllegalArgumentException("Duration is required when duration unit is provided");
        }
    }

    private void validateQuantityPair(Integer quantity, QuantityUnit unit) {
        if (quantity != null && unit == null) {
            throw new IllegalArgumentException("Quantity unit is required when quantity is provided");
        }
        if (unit != null && quantity == null) {
            throw new IllegalArgumentException("Quantity is required when quantity unit is provided");
        }
    }

    private void validateFrequencyPair(MedicationFrequency frequency,
                                       Integer frequencyValue, FrequencyUnit frequencyUnit) {
        if (frequencyValue != null && frequencyUnit == null) {
            throw new IllegalArgumentException("Frequency unit is required when frequency value is provided");
        }
        if (frequencyUnit != null && frequencyValue == null) {
            throw new IllegalArgumentException("Frequency value is required when frequency unit is provided");
        }
    }

    private void validateCustomFrequencyInstructions(MedicationFrequency frequency, String instructions) {
        if (frequency == MedicationFrequency.CUSTOM
                && (instructions == null || instructions.isBlank())) {
            throw new IllegalArgumentException(
                    "Instructions are required when frequency is CUSTOM");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PrescriptionItemResponse toResponse(PrescriptionItem item) {
        Medication medication = item.getMedication();
        return PrescriptionItemResponse.builder()
                .id(item.getId())
                .uuid(item.getUuid())
                .prescriptionUuid(item.getPrescription().getUuid())
                .medicationUuid(medication.getUuid())
                .medicationCode(medication.getCode())
                .medicationGenericName(medication.getGenericName())
                .medicationBrandName(medication.getBrandName())
                .medicationStrength(medication.getStrength())
                .medicationStrengthUnit(medication.getStrengthUnit())
                .medicationDosageForm(medication.getDosageForm())
                .medicationRoute(medication.getRoute())
                .dose(item.getDose())
                .doseUnit(item.getDoseUnit())
                .route(item.getRoute())
                .frequency(item.getFrequency())
                .frequencyValue(item.getFrequencyValue())
                .frequencyUnit(item.getFrequencyUnit())
                .duration(item.getDuration())
                .durationUnit(item.getDurationUnit())
                .quantity(item.getQuantity())
                .quantityUnit(item.getQuantityUnit())
                .refills(item.getRefills())
                .instructions(item.getInstructions())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .version(item.getVersion())
                .build();
    }
}
