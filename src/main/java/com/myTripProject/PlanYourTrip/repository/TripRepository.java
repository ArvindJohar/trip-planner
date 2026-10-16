
package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Existing methods — kept for compatibility
    Page<Trip> findByTitleContainingIgnoreCase(
            String title, Pageable pageable);

    Page<Trip> findByStartDateBetween(
            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<Trip> findByTitleContainingIgnoreCaseAndStartDateBetween(
            String title, LocalDate fromDate, LocalDate toDate,
            Pageable pageable);

    // New methods — retrieve trips belonging to a specific user
    Page<Trip> findByUserId(
            Long userId, Pageable pageable);

    Page<Trip> findByUserIdAndTitleContainingIgnoreCase(
            Long userId, String title, Pageable pageable);

    Page<Trip> findByUserIdAndStartDateBetween(
            Long userId, LocalDate fromDate, LocalDate toDate,
            Pageable pageable);

    Page<Trip> findByUserIdAndTitleContainingIgnoreCaseAndStartDateBetween(
            Long userId, String title, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);
}
