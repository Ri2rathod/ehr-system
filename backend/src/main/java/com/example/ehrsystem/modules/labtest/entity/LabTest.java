package com.example.ehrsystem.modules.labtest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lab test catalog entry - patient and encounter independent reference data.
 * Terminology-ready: code + codeSystem identify the entry in LOINC/local systems.
 */
@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "code_system", length = 50)
    private String codeSystem;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "specimen_type", length = 30)
    private SpecimenType specimenType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private ResultType resultType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "default_reference_low", precision = 14, scale = 4)
    private BigDecimal defaultReferenceLow;

    @Column(name = "default_reference_high", precision = 14, scale = 4)
    private BigDecimal defaultReferenceHigh;

    @Column(name = "default_reference_text", length = 300)
    private String defaultReferenceText;

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
        LabTest labTest = (LabTest) o;
        return uuid != null && uuid.equals(labTest.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
