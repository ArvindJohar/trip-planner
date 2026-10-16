package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    List<Destination> findByTripIdOrderBySortOrderAscIdAsc(Long tripId);
}