package com.appointmentbooking.domain.model;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "appointment_audit_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AppointmentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_audit_appointment"))
    private Appointment appointment;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private AppointmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private AppointmentStatus newStatus;

    @Column(name = "changed_by", length = 100)
    @Builder.Default
    private String changedBy = "system";

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime changedAt = OffsetDateTime.now();
}
