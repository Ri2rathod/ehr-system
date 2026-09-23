package com.example.ehrsystem.modules.appointment.repository;

import com.example.ehrsystem.modules.appointment.entity.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {

    Optional<AppointmentHistory> findByUuid(UUID uuid);

    List<AppointmentHistory> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    List<AppointmentHistory> findByAppointmentUuidOrderByCreatedAtDesc(UUID appointmentUuid);
}
