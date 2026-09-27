package com.example.ehrsystem.modules.laborder.repository;

import com.example.ehrsystem.modules.laborder.entity.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {

    Optional<LabOrderItem> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<LabOrderItem> findByUuidAndLabOrderIdAndDeletedAtIsNull(UUID uuid, Long labOrderId);

    List<LabOrderItem> findByLabOrderIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long labOrderId);

    long countByLabOrderIdAndDeletedAtIsNull(Long labOrderId);

    long countByLabTestIdAndDeletedAtIsNull(Long labTestId);

    boolean existsByLabOrderIdAndLabTestIdAndDeletedAtIsNull(Long labOrderId, Long labTestId);
}
