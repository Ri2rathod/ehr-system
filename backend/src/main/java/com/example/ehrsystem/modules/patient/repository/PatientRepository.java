package com.example.ehrsystem.modules.patient.repository;

import com.example.ehrsystem.modules.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndDeletedAtIsNull(Long id);

    Optional<Patient> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Patient> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Patient> findByMrnAndDeletedAtIsNull(String mrn);

    Optional<Patient> findByEmailAndDeletedAtIsNull(String email);

    Optional<Patient> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.phoneNumber LIKE CONCAT('%', :query, '%'))")
    Page<Patient> searchPatients(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND p.status = :status")
    Page<Patient> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL")
    Page<Patient> findAllActive(Pageable pageable);

    boolean existsByMrnAndDeletedAtIsNull(String mrn);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
}