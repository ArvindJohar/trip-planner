package com.myTripProject.PlanYourTrip.controller;
import com.myTripProject.PlanYourTrip.dto.DestinationPatchRequest2;
import com.myTripProject.PlanYourTrip.dto.DestinationRequest;
import com.myTripProject.PlanYourTrip.dto.DestinationResponse;
import com.myTripProject.PlanYourTrip.service.DestinationService;
import jakarta.validation.Valid;
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
    public DestinationResponse createDestination(
            @PathVariable Long tripId,
            @Valid @RequestBody DestinationRequest request) {

        return destinationService.createDestination(tripId, request);
    }

    @GetMapping("/trips/{tripId}/destinations")
    public List<DestinationResponse> getDestinations(@PathVariable Long tripId) {
        return destinationService.getDestinations(tripId);
    }

    @GetMapping("/destinations/{destinationId}")
    public DestinationResponse getDestinationById(@PathVariable Long destinationId) {
        return destinationService.getDestinationById(destinationId);
    }

    @DeleteMapping("/destinations/{destinationId}")
    public void deleteDestinationById(@PathVariable Long destinationId) {
        destinationService.deleteDestinationById(destinationId);
    }

    @PatchMapping("/destinations/{destinationId}")
        public DestinationResponse updateDestinationById(@PathVariable Long destinationId, @RequestBody DestinationPatchRequest2 request) {
            return destinationService.updateDestinationById(destinationId, request);
        }
}
