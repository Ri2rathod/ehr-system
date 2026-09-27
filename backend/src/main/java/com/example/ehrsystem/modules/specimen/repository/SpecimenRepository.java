package com.example.ehrsystem.modules.specimen.repository;

import com.example.ehrsystem.modules.specimen.entity.Specimen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecimenRepository extends JpaRepository<Specimen, Long> {

    Optional<Specimen> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Specimen> findByUuidAndLabOrderIdAndDeletedAtIsNull(UUID uuid, Long labOrderId);

    List<Specimen> findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long labOrderId);
}
