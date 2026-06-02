package com.example.ehrsystem.modules.appointment.repository;

import com.example.ehrsystem.modules.appointment.entity.AppointmentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AppointmentSequenceRepository extends JpaRepository<AppointmentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AppointmentSequence a WHERE a.sequenceYear = :year")
    Optional<AppointmentSequence> findByYearWithLock(@Param("year") Integer year);
}
