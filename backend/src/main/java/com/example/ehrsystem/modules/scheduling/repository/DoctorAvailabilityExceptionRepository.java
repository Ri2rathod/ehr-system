package com.example.ehrsystem.modules.scheduling.repository;

import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorAvailabilityExceptionRepository extends JpaRepository<DoctorAvailabilityException, Long> {

    Optional<DoctorAvailabilityException> findByUuid(UUID uuid);

    /**
     * All exceptions for a doctor (management view).
     */
    List<DoctorAvailabilityException> findByDoctorIdOrderByExceptionDateDesc(Long doctorId);

    /**
     * Exceptions for a specific date (used by slot generation).
     */
    @Query("SELECT e FROM DoctorAvailabilityException e " +
           "WHERE e.doctor.id = :doctorId AND e.exceptionDate = :date")
    List<DoctorAvailabilityException> findByDoctorIdAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    /**
     * Check if there is a full-day exception on a given date.
     */
    @Query("SELECT COUNT(e) > 0 FROM DoctorAvailabilityException e " +
           "WHERE e.doctor.id = :doctorId " +
           "AND e.exceptionDate = :date " +
           "AND e.isFullDay = true")
    boolean existsFullDayException(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    /**
     * Exceptions within a date range (for calendar views).
     */
    @Query("SELECT e FROM DoctorAvailabilityException e " +
           "WHERE e.doctor.id = :doctorId " +
           "AND e.exceptionDate BETWEEN :fromDate AND :toDate " +
           "ORDER BY e.exceptionDate")
    List<DoctorAvailabilityException> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
