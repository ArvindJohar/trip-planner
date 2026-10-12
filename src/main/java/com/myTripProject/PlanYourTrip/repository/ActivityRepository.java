package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByDestinationId(Long destinationId);
    List<Activity> findByDestinationTripId(Long tripId);
}
