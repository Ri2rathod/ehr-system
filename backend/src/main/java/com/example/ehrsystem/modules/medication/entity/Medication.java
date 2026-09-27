package com.example.ehrsystem.modules.medication.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medication catalog entry - patient and encounter independent reference data.
 * Terminology-ready: code + codeSystem identify the entry in RxNorm/NDC/local systems.
 */
@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "code_system", length = 50)
    private String codeSystem;

    @Column(name = "generic_name", nullable = false, length = 300)
    private String genericName;

    @Column(name = "brand_name", length = 300)
    private String brandName;

    @Column(name = "strength", precision = 10, scale = 3)
    private BigDecimal strength;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength_unit", length = 20)
    private DoseUnit strengthUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_form", length = 30)
    private DosageForm dosageForm;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", length = 30)
    private Route route;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

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
        if (isActive == null) isActive = Boolean.TRUE;
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
        Medication medication = (Medication) o;
        return uuid != null && uuid.equals(medication.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
