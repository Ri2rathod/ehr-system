package com.example.ehrsystem.modules.labresult.entity;

import com.example.ehrsystem.modules.labtest.entity.LabTest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One measured component of a lab result. Exactly one of
 * valueNumeric/valueText/valueCode is stored (DB CHECK enforced).
 * The reference range is copied from the catalog at result time so
 * historical results keep the range they were evaluated against.
 */
@Entity
@Table(name = "lab_result_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_result_id", nullable = false)
    private LabResult labResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(name = "value_numeric", precision = 14, scale = 4)
    private BigDecimal valueNumeric;

    @Column(name = "value_text", length = 500)
    private String valueText;

    @Column(name = "value_code", length = 100)
    private String valueCode;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_low", precision = 14, scale = 4)
    private BigDecimal referenceLow;

    @Column(name = "reference_high", precision = 14, scale = 4)
    private BigDecimal referenceHigh;

    @Column(name = "reference_text", length = 300)
    private String referenceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "abnormal_flag", length = 30)
    private AbnormalFlag abnormalFlag;

    @Column(name = "notes")
    private String notes;

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
        LabResultValue that = (LabResultValue) o;
        return uuid != null && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
