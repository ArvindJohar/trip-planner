package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("""
        SELECT MAX(a.sortOrder)
        FROM Activity a
        WHERE a.destination.id = :destinationId
        """)
    Integer findMaxSortOrderByDestinationId(
            @Param("destinationId") Long destinationId);

    List<Activity> findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
            Long destinationId);

    List<Activity> findByDestinationTripIdOrderByDateAscStartTimeAscSortOrderAsc(
            Long tripId);
}