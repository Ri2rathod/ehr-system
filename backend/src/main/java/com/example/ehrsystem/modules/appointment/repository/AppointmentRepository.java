package com.example.ehrsystem.modules.appointment.repository;

import com.example.ehrsystem.modules.appointment.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Appointment> findByAppointmentNumberAndDeletedAtIsNull(String appointmentNumber);

    Page<Appointment> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.deletedAt IS NULL AND a.status NOT IN ('CANCELLED') " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND (:excludeUuid IS NULL OR a.uuid != :excludeUuid)")
    boolean existsOverlappingDoctorAppointment(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeUuid") UUID excludeUuid
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.deletedAt IS NULL AND a.status NOT IN ('CANCELLED') " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND (:excludeUuid IS NULL OR a.uuid != :excludeUuid)")
    boolean existsOverlappingPatientAppointment(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeUuid") UUID excludeUuid
    );

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.appointmentDate = :date ORDER BY a.startTime ASC")
    List<Appointment> findByAppointmentDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.doctor.uuid = :doctorUuid " +
           "AND a.startTime >= :from AND a.startTime <= :to " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findByDoctorUuidAndDateRange(
            @Param("doctorUuid") UUID doctorUuid,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.patient.uuid = :patientUuid " +
           "AND a.startTime >= :from AND a.startTime <= :to " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findByPatientUuidAndDateRange(
            @Param("patientUuid") UUID patientUuid,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
