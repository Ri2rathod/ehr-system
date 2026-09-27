package com.example.ehrsystem.modules.prescription.repository;

import com.example.ehrsystem.modules.prescription.entity.PrescriptionSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionSequenceRepository extends JpaRepository<PrescriptionSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PrescriptionSequence p WHERE p.sequenceYear = :year")
    Optional<PrescriptionSequence> findByYearWithLock(@Param("year") Integer year);

    Optional<PrescriptionSequence> findBySequenceYear(Integer sequenceYear);
}
