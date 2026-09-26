package com.example.ehrsystem.modules.diagnosis.repository;

import com.example.ehrsystem.modules.diagnosis.entity.Diagnosis;
import com.example.ehrsystem.modules.diagnosis.entity.DiagnosisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Diagnosis> findByUuidAndEncounterIdAndDeletedAtIsNull(UUID uuid, Long encounterId);

    List<Diagnosis> findByEncounterIdAndDeletedAtIsNull(Long encounterId);

    Optional<Diagnosis> findFirstByEncounterIdAndDiagnosisTypeAndDeletedAtIsNull(
            Long encounterId, DiagnosisType diagnosisType);

    boolean existsByEncounterIdAndDiagnosisTypeAndDeletedAtIsNull(Long encounterId, DiagnosisType diagnosisType);
}
