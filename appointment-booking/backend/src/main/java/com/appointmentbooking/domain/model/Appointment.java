package com.appointmentbooking.domain.model;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;


@Entity
@Table(
    name = "appointments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_appointments_reference",
        columnNames = "reference_number"
    ),
    indexes = {
        @Index(name = "idx_appt_branch_date",  columnList = "branch_id, appointment_date"),
        @Index(name = "idx_appt_email",         columnList = "customer_email"),
        @Index(name = "idx_appt_status",        columnList = "status"),
        @Index(name = "idx_appt_reference",     columnList = "reference_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "reference_number", nullable = false, length = 20)
    private String referenceNumber;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    /**
     * AES-256-CBC encrypted SA ID number.
     * Format stored: "ivHex:base64Ciphertext"
     * Never expose the raw value in API responses.
     */
    @Column(name = "id_number", columnDefinition = "TEXT")
    private String idNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_branch"))
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    /**
     * True once the ~1-hour "appointment reminder" email has been sent.
     * Guarded by the reminder scheduler so a customer is reminded only once.
     */
    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
