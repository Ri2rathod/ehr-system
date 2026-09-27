package com.example.ehrsystem.modules.laborder.repository;

import com.example.ehrsystem.modules.laborder.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    Optional<LabOrder> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<LabOrder> findByUuidAndEncounterIdAndDeletedAtIsNull(UUID uuid, Long encounterId);

    List<LabOrder> findByEncounterIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long encounterId);
}
