package com.example.ehrsystem.modules.doctor.repository;

import com.example.ehrsystem.modules.doctor.entity.DoctorSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorSequenceRepository extends JpaRepository<DoctorSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DoctorSequence ds WHERE ds.sequenceYear = :year")
    Optional<DoctorSequence> findByYearWithLock(@Param("year") Integer year);

    Optional<DoctorSequence> findBySequenceYear(Integer sequenceYear);
}