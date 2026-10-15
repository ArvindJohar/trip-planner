package com.myTripProject.PlanYourTrip.controller;

import com.myTripProject.PlanYourTrip.dto.ActivityPatchRequest2;
import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.service.ActivityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService activityService;
    ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/destinations/{destinationId}/activities")
    public ResponseEntity<ActivityResponse> createActivity(
            @PathVariable
            @Positive(message = "Destination id must be positive.")
            Long destinationId,

            @RequestParam
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE
            )
            java.time.LocalDate date,

            @Valid @RequestBody ActivityRequest request) {

        ActivityResponse activityResponse =
                activityService.createActivity(destinationId, date, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(activityResponse);
    }

    @GetMapping("/destinations/{destinationId}/activities")
    public ResponseEntity<List<ActivityResponse>> getActivities(@PathVariable @Positive(message = "Destination id must be positive.") Long destinationId) {
        List<ActivityResponse> activityResponses = activityService.getActivities(destinationId);
        return ResponseEntity.ok(activityResponses);
    }

    @GetMapping("/trips/{tripId}/activities")
    public ResponseEntity<List<ActivityResponse>> getActivities2(@PathVariable @Positive(message = "Trip id must be positive.") Long tripId) {
        List<ActivityResponse> activityResponses = activityService.getActivities2(tripId);
        return ResponseEntity.ok(activityResponses);
    }

    @GetMapping("/activities/{activityId}")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable @Positive(message = "Activity id must be positive.") Long activityId) {
        ActivityResponse activityResponse = activityService.getActivityById(activityId);
        return ResponseEntity.ok(activityResponse);
    }

    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<Void> deleteActivityById(@PathVariable @Positive(message = "Activity id must be positive.") Long activityId) {
        activityService.deleteActivityById(activityId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/activities/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivityById(@PathVariable @Positive(message = "Activity id must be positive.") Long activityId, @RequestBody ActivityPatchRequest2 request) {
        return ResponseEntity.ok(activityService.updateActivityById(activityId, request));
    }
}
