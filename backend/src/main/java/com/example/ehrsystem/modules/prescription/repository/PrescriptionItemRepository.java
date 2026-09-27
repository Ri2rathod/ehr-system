package com.example.ehrsystem.modules.prescription.repository;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    Optional<PrescriptionItem> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<PrescriptionItem> findByUuidAndPrescriptionIdAndDeletedAtIsNull(
            UUID uuid, Long prescriptionId);

    List<PrescriptionItem> findByPrescriptionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long prescriptionId);

    long countByPrescriptionIdAndDeletedAtIsNull(Long prescriptionId);
}
