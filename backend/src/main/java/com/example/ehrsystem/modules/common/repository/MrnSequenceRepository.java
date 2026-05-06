package com.example.ehrsystem.modules.common.repository;

import com.example.ehrsystem.modules.common.entity.MrnSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface MrnSequenceRepository extends JpaRepository<MrnSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MrnSequence m WHERE m.sequenceYear = :year")
    Optional<MrnSequence> findByYearWithLock(@Param("year") Integer year);

    Optional<MrnSequence> findBySequenceYear(Integer sequenceYear);
}