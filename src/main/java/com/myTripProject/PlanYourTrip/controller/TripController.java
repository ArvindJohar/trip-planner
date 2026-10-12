package com.myTripProject.PlanYourTrip.controller;

import com.myTripProject.PlanYourTrip.dto.BudgetSummaryResponse;
import com.myTripProject.PlanYourTrip.dto.TripRequest;
import com.myTripProject.PlanYourTrip.dto.TripResponse;
import com.myTripProject.PlanYourTrip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public TripResponse createTrip(@Valid @RequestBody TripRequest request) {
        return tripService.createTrip(request);
    }

    @GetMapping
    public Page<TripResponse> getTrips(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return tripService.getTrips(title, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    public TripResponse getTripById(@PathVariable Long id) {
        return tripService.getTripById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTripById(@PathVariable Long id) {
        tripService.deleteTripById(id);
    }

    @GetMapping("/{id}/budget-summary")
    public BudgetSummaryResponse getBudgetSummary(@PathVariable Long id) {
        return tripService.getBudgetSummary(id);
    }
}