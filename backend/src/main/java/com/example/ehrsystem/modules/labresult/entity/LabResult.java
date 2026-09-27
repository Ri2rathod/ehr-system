package com.example.ehrsystem.modules.labresult.entity;

import com.example.ehrsystem.modules.laborder.entity.LabOrderItem;
import com.example.ehrsystem.modules.specimen.entity.Specimen;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One recorded result for a lab order item against a specific specimen.
 * PRELIMINARY -> FINAL -> CORRECTED; PRELIMINARY -> CANCELLED.
 * Finalized results are never deleted; corrections replace the values
 * and record a mandatory correction reason.
 */
@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_item_id", nullable = false)
    private LabOrderItem labOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specimen_id", nullable = false)
    private Specimen specimen;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LabResultStatus status = LabResultStatus.PRELIMINARY;

    @Column(name = "resulted_at", nullable = false)
    private LocalDateTime resultedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "comments")
    private String comments;

    @Column(name = "correction_reason", length = 1000)
    private String correctionReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (status == null) status = LabResultStatus.PRELIMINARY;
        if (resultedAt == null) resultedAt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabResult labResult = (LabResult) o;
        return uuid != null && uuid.equals(labResult.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
