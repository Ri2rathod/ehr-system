package com.example.ehrsystem.modules.scheduling.repository;

import com.example.ehrsystem.modules.scheduling.entity.DayOfWeekEnum;
import com.example.ehrsystem.modules.scheduling.entity.DoctorAvailability;
import com.example.ehrsystem.modules.scheduling.entity.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    Optional<DoctorAvailability> findByUuid(UUID uuid);

    /**
     * Full weekly schedule for a doctor (active only).
     */
    @Query("SELECT da FROM DoctorAvailability da " +
           "WHERE da.doctor.id = :doctorId AND da.isActive = true " +
           "ORDER BY da.dayOfWeek, da.startTime")
    List<DoctorAvailability> findActiveByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Schedule for a specific day, filtered by effective date range.
     * Used by the slot generation service.
     */
    @Query("SELECT da FROM DoctorAvailability da " +
           "WHERE da.doctor.id = :doctorId " +
           "AND da.dayOfWeek = :dayOfWeek " +
           "AND da.isActive = true " +
           "AND (da.effectiveFrom IS NULL OR da.effectiveFrom <= :date) " +
           "AND (da.effectiveUntil IS NULL OR da.effectiveUntil >= :date) " +
           "ORDER BY da.startTime")
    List<DoctorAvailability> findEffectiveByDoctorAndDay(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeekEnum dayOfWeek,
            @Param("date") LocalDate date);

    /**
     * WORK-only periods for a specific day (used for slot generation).
     */
    @Query("SELECT da FROM DoctorAvailability da " +
           "WHERE da.doctor.id = :doctorId " +
           "AND da.dayOfWeek = :dayOfWeek " +
           "AND da.scheduleType = :scheduleType " +
           "AND da.isActive = true " +
           "AND (da.effectiveFrom IS NULL OR da.effectiveFrom <= :date) " +
           "AND (da.effectiveUntil IS NULL OR da.effectiveUntil >= :date) " +
           "ORDER BY da.startTime")
    List<DoctorAvailability> findEffectiveByDoctorDayAndType(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeekEnum dayOfWeek,
            @Param("scheduleType") ScheduleType scheduleType,
            @Param("date") LocalDate date);

    /**
     * All availability rows for a doctor (including inactive — for management views).
     */
    List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    /**
     * Check whether a doctor has any active availability configured.
     */
    boolean existsByDoctorIdAndIsActiveTrue(Long doctorId);
}
