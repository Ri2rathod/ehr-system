package com.example.ehrsystem.modules.doctor.repository;

import com.example.ehrsystem.modules.doctor.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByIdAndDeletedAtIsNull(Long id);

    Optional<Doctor> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Doctor> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Doctor> findByDoctorCodeAndDeletedAtIsNull(String doctorCode);

    Optional<Doctor> findByEmailAndDeletedAtIsNull(String email);

    Optional<Doctor> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.doctorCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "d.phoneNumber LIKE CONCAT('%', :query, '%'))")
    Page<Doctor> searchDoctors(@Param("query") String query, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL AND d.status = :status")
    Page<Doctor> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL AND d.specialization = :specialization AND d.status = 'ACTIVE'")
    Page<Doctor> findBySpecializationAndActive(@Param("specialization") String specialization, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL AND d.status = 'ACTIVE' AND d.isAvailableForAppointments = true")
    Page<Doctor> findAllAvailable(Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.deletedAt IS NULL")
    Page<Doctor> findAllActive(Pageable pageable);

    boolean existsByDoctorCodeAndDeletedAtIsNull(String doctorCode);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
}