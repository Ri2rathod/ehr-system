package com.example.ehrsystem.modules.labtest.repository;

import com.example.ehrsystem.modules.labtest.entity.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface LabTestRepository
        extends JpaRepository<LabTest, Long>, JpaSpecificationExecutor<LabTest> {

    Optional<LabTest> findByUuidAndDeletedAtIsNull(UUID uuid);

    boolean existsByCodeAndCodeSystemAndDeletedAtIsNull(String code, String codeSystem);

    boolean existsByCodeAndCodeSystemIsNullAndDeletedAtIsNull(String code);
}
