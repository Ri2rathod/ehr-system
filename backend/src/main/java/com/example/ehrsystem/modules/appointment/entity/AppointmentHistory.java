package com.example.ehrsystem.modules.appointment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "appointment_number", nullable = false, length = 50)
    private String appointmentNumber;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "previous_start_time")
    private LocalDateTime previousStartTime;

    @Column(name = "previous_end_time")
    private LocalDateTime previousEndTime;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_start_time")
    private LocalDateTime newStartTime;

    @Column(name = "new_end_time")
    private LocalDateTime newEndTime;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
