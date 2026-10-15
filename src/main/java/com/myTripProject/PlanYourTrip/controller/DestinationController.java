package com.myTripProject.PlanYourTrip.controller;
import com.myTripProject.PlanYourTrip.dto.DestinationPatchRequest2;
import com.myTripProject.PlanYourTrip.dto.DestinationRequest;
import com.myTripProject.PlanYourTrip.dto.DestinationResponse;
import com.myTripProject.PlanYourTrip.service.DestinationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DestinationController {
    private final DestinationService destinationService;
    DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }
    @PostMapping("/trips/{tripId}/destinations")
    public ResponseEntity<DestinationResponse> createDestination(
            @PathVariable @Positive(message = "Trip id must be positive.") Long tripId,
            @Valid @RequestBody DestinationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(destinationService.createDestination(tripId, request));
    }

    @GetMapping("/trips/{tripId}/destinations")
    public ResponseEntity<List<DestinationResponse>> getDestinations(@PathVariable @Positive(message = "Trip id must be positive.") Long tripId) {
        return ResponseEntity.ok(destinationService.getDestinations(tripId));
    }

    @GetMapping("/destinations/{destinationId}")
    public ResponseEntity<DestinationResponse> getDestinationById(@PathVariable @Positive(message = "Destination id must be positive.") Long destinationId) {
        return ResponseEntity.ok(destinationService.getDestinationById(destinationId));
    }

    @DeleteMapping("/destinations/{destinationId}")
    public ResponseEntity<Void> deleteDestinationById(@PathVariable @Positive(message = "Destination id must be positive.") Long destinationId) {
        destinationService.deleteDestinationById(destinationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/destinations/{destinationId}")
        public ResponseEntity<DestinationResponse> updateDestinationById(@PathVariable @Positive(message = "Destination id must be positive.") Long destinationId, @RequestBody DestinationPatchRequest2 request) {
            return ResponseEntity.ok(destinationService.updateDestinationById(destinationId, request));
        }
}
