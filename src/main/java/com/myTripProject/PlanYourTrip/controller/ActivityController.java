package com.myTripProject.PlanYourTrip.controller;

import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.service.ActivityService;
import jakarta.validation.Valid;
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
    public ActivityResponse createActivity(@PathVariable Long destinationId, @Valid @RequestBody ActivityRequest request) {
        return activityService.createActivity(destinationId, request);
    }

    @GetMapping("/destinations/{destinationId}/activities")
    public List<ActivityResponse> getActivities(@PathVariable Long destinationId) {
        return activityService.getActivities(destinationId);
    }

    @GetMapping("/trips/{tripId}/activities")
    public List<ActivityResponse> getActivities2(@PathVariable Long tripId) {
        return activityService.getActivities2(tripId);
    }

    @GetMapping("/activities/{activityId}")
    public ActivityResponse getActivityById(@PathVariable Long activityId) {
        return activityService.getActivityById(activityId);
    }

    @DeleteMapping("/activities/{activityId}")
    public void deleteActivityById(@PathVariable Long activityId) {
        activityService.deleteActivityById(activityId);
    }

    @PatchMapping("/activities/{activityId}")
    public ActivityResponse updateActivityById(@PathVariable Long activityId, @RequestBody ActivityRequest request) {
        return activityService.updateActivityById(activityId, request);
    }
}
