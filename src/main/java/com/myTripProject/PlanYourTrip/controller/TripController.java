package com.myTripProject.PlanYourTrip.controller;

import com.myTripProject.PlanYourTrip.dto.*;
import com.myTripProject.PlanYourTrip.service.TripService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    @GetMapping
    public ResponseEntity<Page<TripResponse>> getTrips(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Page number cannot be negative.") int page,
            @RequestParam(defaultValue = "10") @Positive(message = "Page size must be positive.") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(tripService.getTrips(title, fromDate, toDate, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable @Positive(message = "Trip id must be positive.") Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripById(@PathVariable @Positive(message = "Trip id must be positive.")Long id) {
        tripService.deleteTripById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/budget-summary")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(@PathVariable @Positive(message = "Trip id must be positive.")Long id) {
        return ResponseEntity.ok(tripService.getBudgetSummary(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTripById(@PathVariable @Positive(message = "Trip id must be positive.") Long id, @RequestBody TripUpdateRequest request) {
        return ResponseEntity.ok(tripService.replaceTrip(id, request));
    }
}