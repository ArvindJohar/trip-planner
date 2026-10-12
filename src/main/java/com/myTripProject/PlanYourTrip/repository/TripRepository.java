package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Page<Trip> findByTitleContainingIgnoreCase(
            String title, Pageable pageable);

    Page<Trip> findByStartDateBetween(
            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<Trip> findByTitleContainingIgnoreCaseAndStartDateBetween(
            String title, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
