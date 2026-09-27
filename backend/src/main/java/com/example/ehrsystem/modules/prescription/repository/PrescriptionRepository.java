package com.example.ehrsystem.modules.prescription.repository;

import com.example.ehrsystem.modules.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Prescription> findByUuidAndEncounterIdAndDeletedAtIsNull(UUID uuid, Long encounterId);

    List<Prescription> findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long encounterId);

    boolean existsByEncounterIdAndDeletedAtIsNull(Long encounterId);
}
