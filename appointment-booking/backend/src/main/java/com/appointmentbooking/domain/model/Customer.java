package com.appointmentbooking.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;


@Entity
@Table(
    name = "customers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_customers_id_hash", columnNames = "id_number_hash"),
        @UniqueConstraint(name = "uq_customers_email",   columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_customers_id_hash", columnList = "id_number_hash")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"pinHash", "idNumberHash", "idNumberEncrypted"})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** SHA-256 hex digest of the plain ID number — lookup index only. */
    @Column(name = "id_number_hash", nullable = false, length = 64)
    private String idNumberHash;

    /** AES-256-CBC encrypted ID number. Format: "ivHex:base64Ciphertext" */
    @Column(name = "id_number_encrypted", nullable = false, columnDefinition = "TEXT")
    private String idNumberEncrypted;

    /** bcrypt hash of the customer's 5-digit remote PIN. */
    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
