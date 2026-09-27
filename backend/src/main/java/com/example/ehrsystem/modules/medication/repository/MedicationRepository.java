package com.example.ehrsystem.modules.medication.repository;

import com.example.ehrsystem.modules.medication.entity.DosageForm;
import com.example.ehrsystem.modules.medication.entity.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    Optional<Medication> findByUuidAndDeletedAtIsNull(UUID uuid);

    boolean existsByCodeAndCodeSystemAndDeletedAtIsNull(String code, String codeSystem);

    /** Active catalog entries for prescribing (name/brand/code match). */
    @Query("SELECT m FROM Medication m WHERE m.deletedAt IS NULL AND m.isActive = TRUE AND " +
           "(LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.brandName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:dosageForm IS NULL OR m.dosageForm = :dosageForm)")
    Page<Medication> searchActiveMedications(
            @Param("query") String query,
            @Param("dosageForm") DosageForm dosageForm,
            Pageable pageable);

    /** Any non-deleted entry (admin catalog management). */
    @Query("SELECT m FROM Medication m WHERE m.deletedAt IS NULL AND " +
           "(LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.brandName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Medication> searchMedications(@Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM Medication m WHERE m.deletedAt IS NULL AND " +
           "(:dosageForm IS NULL OR m.dosageForm = :dosageForm)")
    Page<Medication> findAllNotDeleted(
            @Param("dosageForm") DosageForm dosageForm, Pageable pageable);
}
