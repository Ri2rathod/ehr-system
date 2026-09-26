package com.example.ehrsystem.modules.encounter.repository;

import com.example.ehrsystem.modules.encounter.entity.Encounter;
import com.example.ehrsystem.modules.encounter.entity.EncounterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long>, JpaSpecificationExecutor<Encounter> {

    Optional<Encounter> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Encounter> findByEncounterNumberAndDeletedAtIsNull(String encounterNumber);

    Page<Encounter> findByDeletedAtIsNull(Pageable pageable);

    List<Encounter> findByAppointmentUuidAndDeletedAtIsNull(UUID appointmentUuid);

    boolean existsByAppointmentIdAndDeletedAtIsNullAndStatusNot(Long appointmentId, EncounterStatus status);
}
