package com.example.ehrsystem.modules.appointment.repository;

import com.example.ehrsystem.modules.appointment.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<Appointment> findByAppointmentNumberAndDeletedAtIsNull(String appointmentNumber);

    Page<Appointment> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.deletedAt IS NULL AND a.status NOT IN ('CANCELLED', 'RESCHEDULED') " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND (:excludeUuid IS NULL OR a.uuid != :excludeUuid)")
    boolean existsOverlappingDoctorAppointment(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeUuid") UUID excludeUuid
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.deletedAt IS NULL AND a.status NOT IN ('CANCELLED', 'RESCHEDULED') " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND (:excludeUuid IS NULL OR a.uuid != :excludeUuid)")
    boolean existsOverlappingPatientAppointment(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeUuid") UUID excludeUuid
    );
}
