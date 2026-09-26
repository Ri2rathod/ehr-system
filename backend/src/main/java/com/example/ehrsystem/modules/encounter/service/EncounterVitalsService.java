package com.example.ehrsystem.modules.encounter.service;

import com.example.ehrsystem.common.security.SecurityContextAccessor;
import com.example.ehrsystem.common.util.AuditLogger;
import com.example.ehrsystem.modules.encounter.dto.request.RecordVitalsRequest;
import com.example.ehrsystem.modules.encounter.dto.request.UpdateVitalsRequest;
import com.example.ehrsystem.modules.encounter.dto.response.EncounterVitalsResponse;
import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import com.example.ehrsystem.modules.encounter.entity.EncounterVitals;
import com.example.ehrsystem.modules.encounter.repository.EncounterRepository;
import com.example.ehrsystem.modules.encounter.repository.EncounterVitalsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EncounterVitalsService {

    private final EncounterVitalsRepository vitalsRepository;
    private final EncounterRepository encounterRepository;
    private final SecurityContextAccessor securityContext;
    private final AuditLogger auditLogger;

    @Transactional
    public EncounterVitalsResponse record(UUID encounterUuid, RecordVitalsRequest request) {
        Encounter encounter = requireEncounter(encounterUuid);
        assertEncounterActive(encounter);
        assertAnyVitalPresent(request.getTemperature(), request.getHeartRate(), request.getRespiratoryRate(),
                request.getSystolicBp(), request.getDiastolicBp(), request.getOxygenSaturation(),
                request.getWeight(), request.getHeight());
        assertBloodPressurePair(request.getSystolicBp(), request.getDiastolicBp());

        LocalDateTime recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now();
        if (recordedAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Recorded time cannot be in the future");
        }

        EncounterVitals vitals = EncounterVitals.builder()
                .encounter(encounter)
                .temperature(request.getTemperature())
                .heartRate(request.getHeartRate())
                .respiratoryRate(request.getRespiratoryRate())
                .systolicBp(request.getSystolicBp())
                .diastolicBp(request.getDiastolicBp())
                .oxygenSaturation(request.getOxygenSaturation())
                .weight(request.getWeight())
                .height(request.getHeight())
                .recordedAt(recordedAt)
                .recordedBy(securityContext.getCurrentUserId())
                .build();

        recomputeBmi(vitals);

        EncounterVitals saved = vitalsRepository.save(vitals);

        auditLogger.logCustomEvent("VITALS_RECORDED", Map.of(
                "encounterNumber", encounter.getEncounterNumber(),
                "vitalsUuid", saved.getUuid().toString()
        ));

        return toResponse(saved);
    }

    public List<EncounterVitalsResponse> list(UUID encounterUuid) {
        requireEncounter(encounterUuid);
        return vitalsRepository.findByEncounterUuidAndDeletedAtIsNullOrderByRecordedAtDesc(encounterUuid).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EncounterVitalsResponse update(UUID encounterUuid, UUID vitalsUuid, UpdateVitalsRequest request) {
        EncounterVitals vitals = vitalsRepository.findByUuidAndDeletedAtIsNull(vitalsUuid)
                .orElseThrow(() -> new EntityNotFoundException("Vitals not found with UUID: " + vitalsUuid));

        if (!vitals.getEncounter().getUuid().equals(encounterUuid)) {
            throw new EntityNotFoundException("Vitals not found with UUID: " + vitalsUuid);
        }

        assertEncounterActive(vitals.getEncounter());
        assertBloodPressurePair(
                request.getSystolicBp() != null ? request.getSystolicBp() : vitals.getSystolicBp(),
                request.getDiastolicBp() != null ? request.getDiastolicBp() : vitals.getDiastolicBp());

        if (request.getVersion() != null && !request.getVersion().equals(vitals.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vitals have been modified by another user. Please refresh and try again.");
        }

        if (request.getTemperature() != null) vitals.setTemperature(request.getTemperature());
        if (request.getHeartRate() != null) vitals.setHeartRate(request.getHeartRate());
        if (request.getRespiratoryRate() != null) vitals.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getSystolicBp() != null) vitals.setSystolicBp(request.getSystolicBp());
        if (request.getDiastolicBp() != null) vitals.setDiastolicBp(request.getDiastolicBp());
        if (request.getOxygenSaturation() != null) vitals.setOxygenSaturation(request.getOxygenSaturation());
        if (request.getWeight() != null) vitals.setWeight(request.getWeight());
        if (request.getHeight() != null) vitals.setHeight(request.getHeight());
        if (request.getRecordedAt() != null) {
            if (request.getRecordedAt().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Recorded time cannot be in the future");
            }
            vitals.setRecordedAt(request.getRecordedAt());
        }

        recomputeBmi(vitals);

        EncounterVitals saved = vitalsRepository.save(vitals);

        auditLogger.logCustomEvent("VITALS_UPDATED", Map.of(
                "encounterNumber", saved.getEncounter().getEncounterNumber(),
                "vitalsUuid", saved.getUuid().toString()
        ));

        return toResponse(saved);
    }

    private Encounter requireEncounter(UUID uuid) {
        return encounterRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found with UUID: " + uuid));
    }

    private void assertEncounterActive(Encounter encounter) {
        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot record vitals for a completed encounter");
        }
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot record vitals for a cancelled encounter");
        }
    }

    private void assertAnyVitalPresent(Object... values) {
        for (Object value : values) {
            if (value != null) return;
        }
        throw new IllegalArgumentException("At least one vital value is required");
    }

    private void assertBloodPressurePair(Integer systolic, Integer diastolic) {
        if (systolic == null && diastolic == null) return;
        if (systolic == null || diastolic == null) {
            throw new IllegalArgumentException("Both systolic and diastolic blood pressure are required together");
        }
        if (systolic < diastolic) {
            throw new IllegalArgumentException("Systolic blood pressure must be greater than or equal to diastolic blood pressure");
        }
    }

    private void recomputeBmi(EncounterVitals vitals) {
        if (vitals.getWeight() != null && vitals.getHeight() != null
                && vitals.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            double heightMeters = vitals.getHeight().doubleValue() / 100.0;
            double bmi = vitals.getWeight().doubleValue() / (heightMeters * heightMeters);
            vitals.setBmi(BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP));
        } else {
            vitals.setBmi(null);
        }
    }

    private EncounterVitalsResponse toResponse(EncounterVitals vitals) {
        return EncounterVitalsResponse.builder()
                .id(vitals.getId())
                .uuid(vitals.getUuid())
                .encounterUuid(vitals.getEncounter().getUuid())
                .temperature(vitals.getTemperature())
                .heartRate(vitals.getHeartRate())
                .respiratoryRate(vitals.getRespiratoryRate())
                .systolicBp(vitals.getSystolicBp())
                .diastolicBp(vitals.getDiastolicBp())
                .oxygenSaturation(vitals.getOxygenSaturation())
                .weight(vitals.getWeight())
                .height(vitals.getHeight())
                .bmi(vitals.getBmi())
                .recordedAt(vitals.getRecordedAt())
                .recordedBy(vitals.getRecordedBy())
                .createdAt(vitals.getCreatedAt())
                .updatedAt(vitals.getUpdatedAt())
                .version(vitals.getVersion())
                .build();
    }
}
