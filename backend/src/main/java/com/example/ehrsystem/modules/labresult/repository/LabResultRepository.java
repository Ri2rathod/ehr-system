package com.example.ehrsystem.modules.labresult.repository;

import com.example.ehrsystem.modules.labresult.entity.LabResult;
import com.example.ehrsystem.modules.labresult.entity.LabResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    Optional<LabResult> findByUuidAndDeletedAtIsNull(UUID uuid);

    /** Scoped lookup: the result must belong to the given lab order. */
    @Query("SELECT r FROM LabResult r WHERE r.uuid = :uuid AND r.deletedAt IS NULL " +
           "AND r.labOrderItem.labOrder.id = :labOrderId")
    Optional<LabResult> findByUuidAndLabOrderIdAndDeletedAtIsNull(
            @Param("uuid") UUID uuid, @Param("labOrderId") Long labOrderId);

    @Query("SELECT r FROM LabResult r WHERE r.labOrderItem.labOrder.id = :labOrderId " +
           "AND r.deletedAt IS NULL ORDER BY r.createdAt ASC")
    List<LabResult> findByLabOrderIdAndDeletedAtIsNull(@Param("labOrderId") Long labOrderId);

    @Query("SELECT COUNT(r) FROM LabResult r WHERE r.labOrderItem.labOrder.id = :labOrderId " +
           "AND r.deletedAt IS NULL")
    long countByLabOrderIdAndDeletedAtIsNull(@Param("labOrderId") Long labOrderId);

    @Query("SELECT COUNT(r) FROM LabResult r WHERE r.labOrderItem.labOrder.id = :labOrderId " +
           "AND r.deletedAt IS NULL AND r.status IN :statuses")
    long countByLabOrderIdAndStatusInAndDeletedAtIsNull(
            @Param("labOrderId") Long labOrderId,
            @Param("statuses") Collection<LabResultStatus> statuses);
}
