package com.example.ehrsystem.modules.treatmentplan.entity;

import com.example.ehrsystem.modules.diagnosis.entity.Diagnosis;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One concrete treatment/care-plan action (e.g. "30-minute walk, 5 days/week").
 * Belongs to exactly one Treatment Plan. Optionally references a Diagnosis
 * of the same encounter. Medication orders are NOT modelled here —
 * those belong to the future Prescription module.
 */
@Entity
@Table(name = "treatment_plan_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan treatmentPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_type", nullable = false, length = 30)
    private TreatmentType treatmentType;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "frequency", length = 200)
    private String frequency;

    @Column(name = "duration")
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", length = 20)
    private DurationUnit durationUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private TreatmentPriority priority = TreatmentPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TreatmentItemStatus status = TreatmentItemStatus.PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

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
        if (treatmentType == null) treatmentType = TreatmentType.OTHER;
        if (priority == null) priority = TreatmentPriority.MEDIUM;
        if (status == null) status = TreatmentItemStatus.PLANNED;
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
        TreatmentPlanItem item = (TreatmentPlanItem) o;
        return uuid != null && uuid.equals(item.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
