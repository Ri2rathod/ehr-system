package com.example.ehrsystem.modules.treatmentplan.repository;

import com.example.ehrsystem.modules.treatmentplan.entity.TreatmentPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanItemRepository extends JpaRepository<TreatmentPlanItem, Long> {

    Optional<TreatmentPlanItem> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<TreatmentPlanItem> findByUuidAndTreatmentPlanIdAndDeletedAtIsNull(
            UUID uuid, Long treatmentPlanId);

    List<TreatmentPlanItem> findByTreatmentPlanIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long treatmentPlanId);

    List<TreatmentPlanItem> findByTreatmentPlanIdAndDeletedAtIsNull(Long treatmentPlanId);
}
