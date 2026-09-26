package com.example.ehrsystem.modules.encounter.repository;

import com.example.ehrsystem.modules.encounter.entity.EncounterSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncounterSequenceRepository extends JpaRepository<EncounterSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EncounterSequence e WHERE e.sequenceYear = :year")
    Optional<EncounterSequence> findByYearWithLock(@Param("year") Integer year);

    Optional<EncounterSequence> findBySequenceYear(Integer sequenceYear);
}
