package com.example.ehrsystem.modules.treatmentplan.repository;

import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long> {

    Optional<TreatmentPlan> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<TreatmentPlan> findByUuidAndEncounterIdAndDeletedAtIsNull(UUID uuid, Long encounterId);

    List<TreatmentPlan> findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long encounterId);

    Optional<TreatmentPlan> findByIdAndDeletedAtIsNull(Long id);
}
