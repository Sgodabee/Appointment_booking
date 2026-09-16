package com.appointmentbooking.repository;

import com.appointmentbooking.domain.enums.AppointmentStatus;
import com.appointmentbooking.domain.enums.ServiceType;
import com.appointmentbooking.domain.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * AppointmentRepository
 *
 * SECURITY: Every query uses JPQL named parameters (:param).
 * No native SQL with string interpolation is used anywhere.
 * Spring Data JPA compiles these to PreparedStatements — SQL injection
 * is structurally impossible regardless of what values are passed.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ── Lookups ────────────────────────────────────────────────────────────

    @Query("SELECT a FROM Appointment a JOIN FETCH a.branch WHERE a.referenceNumber = :ref")
    Optional<Appointment> findByReferenceNumber(@Param("ref") String referenceNumber);

    // ── Slot availability ──────────────────────────────────────────────────

    /**
     * Returns all non-cancelled appointment times for a branch on a date.
     * Used to build the list of taken slots before generating available ones.
     */
    @Query("""
        SELECT a.appointmentTime FROM Appointment a
        WHERE a.branch.id = :branchId
          AND a.appointmentDate = :date
          AND a.status <> :cancelled
        """)
    List<LocalTime> findTakenSlots(
            @Param("branchId")  Long branchId,
            @Param("date")      LocalDate date,
            @Param("cancelled") AppointmentStatus cancelled);

    /**
     * Checks whether a specific slot is already booked (optionally excluding
     * one appointment ID — used for future rescheduling).
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.branch.id        = :branchId
          AND a.appointmentDate  = :date
          AND a.appointmentTime  = :time
          AND a.status          <> :cancelled
          AND (:excludeId IS NULL OR a.id <> :excludeId)
        """)
    boolean isSlotTaken(
            @Param("branchId")  Long branchId,
            @Param("date")      LocalDate date,
            @Param("time")      LocalTime time,
            @Param("cancelled") AppointmentStatus cancelled,
            @Param("excludeId") Long excludeId);

    // ── Admin list with optional filters ──────────────────────────────────

    @Query("""
        SELECT a FROM Appointment a JOIN FETCH a.branch b
        WHERE (:status      IS NULL OR a.status        = :status)
          AND (:branchId    IS NULL OR b.id             = :branchId)
          AND (:date        IS NULL OR a.appointmentDate = :date)
          AND (:serviceType IS NULL OR a.serviceType    = :serviceType)
        ORDER BY a.appointmentDate ASC, a.appointmentTime ASC
        """)
    Page<Appointment> findAllWithFilters(
            @Param("status")      AppointmentStatus status,
            @Param("branchId")    Long branchId,
            @Param("date")        LocalDate date,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable);

    // ── Cancellation ──────────────────────────────────────────────────────

    /**
     * Finds a cancellable appointment — must match reference AND email
     * (ownership verification) and must not already be cancelled/completed.
     */
    @Query("""
        SELECT a FROM Appointment a JOIN FETCH a.branch
        WHERE a.referenceNumber = :ref
          AND a.customerEmail   = :email
          AND a.status NOT IN (:terminalStatuses)
        """)
    Optional<Appointment> findCancellableByReferenceAndEmail(
            @Param("ref")              String referenceNumber,
            @Param("email")            String customerEmail,
            @Param("terminalStatuses") List<AppointmentStatus> terminalStatuses);

    // ── Status update ─────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") AppointmentStatus status);

    // ── Reminders ─────────────────────────────────────────────────────────

    /**
     * Candidate appointments for the ~1-hour reminder: not yet reminded,
     * still active, and falling on/around the current window. The precise
     * "within the next hour" check is applied in the scheduler where the
     * date + time can be combined against the current instant.
     */
    @Query("""
        SELECT a FROM Appointment a JOIN FETCH a.branch
        WHERE a.reminderSent = false
          AND a.status IN :statuses
          AND a.appointmentDate BETWEEN :fromDate AND :toDate
        """)
    List<Appointment> findRemindable(
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate);

    @Modifying
    @Query("UPDATE Appointment a SET a.reminderSent = true WHERE a.id = :id")
    int markReminderSent(@Param("id") Long id);
}
