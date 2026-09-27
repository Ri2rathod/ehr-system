package com.example.ehrsystem.modules.laborder.repository;

import com.example.ehrsystem.modules.laborder.entity.LabOrderSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface LabOrderSequenceRepository extends JpaRepository<LabOrderSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LabOrderSequence s WHERE s.sequenceYear = :year")
    Optional<LabOrderSequence> findByYearWithLock(@Param("year") Integer year);
}
