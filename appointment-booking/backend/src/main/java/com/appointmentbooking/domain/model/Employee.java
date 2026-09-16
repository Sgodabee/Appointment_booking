package com.appointmentbooking.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Represents a Capitec branch employee who can access the admin dashboard.
 *
 * Security notes:
 *  - {@code passwordHash} is a bcrypt hash (cost 10). Never returned in API responses.
 *  - {@code username} is the login identifier (e.g. employee number or email).
 *  - {@code role} is stored for future role-based access control (EMPLOYEE, MANAGER, etc.)
 */
@Entity
@Table(
    name = "employees",
    uniqueConstraints = @UniqueConstraint(name = "uq_employees_username", columnNames = "username"),
    indexes = @Index(name = "idx_employees_username", columnList = "username")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /** bcrypt hash of the employee's password. Never exposed. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private String role = "EMPLOYEE";

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
