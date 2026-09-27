package com.example.ehrsystem.modules.labresult.repository;

import com.example.ehrsystem.modules.labresult.entity.LabResultValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabResultValueRepository extends JpaRepository<LabResultValue, Long> {

    List<LabResultValue> findByLabResultIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long labResultId);

    long countByLabTestIdAndDeletedAtIsNull(Long labTestId);
}
