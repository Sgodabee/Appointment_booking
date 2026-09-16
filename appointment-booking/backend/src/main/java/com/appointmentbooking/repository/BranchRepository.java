package com.appointmentbooking.repository;

import com.appointmentbooking.domain.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /** Return only active branches, ordered by province then name. */
    @Query("SELECT b FROM Branch b WHERE b.isActive = true ORDER BY b.province ASC, b.name ASC")
    List<Branch> findAllActive();

    /** Find an active branch by its primary key. */
    @Query("SELECT b FROM Branch b WHERE b.id = :id AND b.isActive = true")
    Optional<Branch> findActiveById(@Param("id") Long id);
}
