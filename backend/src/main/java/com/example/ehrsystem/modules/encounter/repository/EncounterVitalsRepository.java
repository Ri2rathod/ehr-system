package com.example.ehrsystem.modules.encounter.repository;

import com.example.ehrsystem.modules.encounter.entity.EncounterVitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncounterVitalsRepository extends JpaRepository<EncounterVitals, Long> {

    Optional<EncounterVitals> findByUuidAndDeletedAtIsNull(UUID uuid);

    List<EncounterVitals> findByEncounterUuidAndDeletedAtIsNullOrderByRecordedAtDesc(UUID encounterUuid);
}
