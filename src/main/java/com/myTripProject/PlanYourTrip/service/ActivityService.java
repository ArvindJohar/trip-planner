package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.entity.Activity;
import com.myTripProject.PlanYourTrip.entity.Destination;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.exception.ResourceNotFoundException;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;

    ActivityService(ActivityRepository activityRepository, DestinationRepository destinationRepository, TripRepository tripRepository) {
        this.activityRepository = activityRepository;
        this.destinationRepository = destinationRepository;
        this.tripRepository = tripRepository;
    }
    public ActivityResponse createActivity(Long destinationId, ActivityRequest request) {
        log.info("Creating new activity...");
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No destination with requested id exists"));
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException(
                    "Activity end time must be after start time"
            );
        }
        if (request.getDate().isBefore(destination.getStartDate())
                || request.getDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Activity date must fall within destination dates"
            );
        }
        Activity activity = new Activity();
        activity.setDestination(destination);
        activity.setCategory(request.getCategory());
        activity.setCost(request.getCost());
        activity.setTitle(request.getTitle());
        activity.setDate(request.getDate());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        Activity updatedActivity = activityRepository.save(activity);
        ActivityResponse activityResponse = createActivityResponse(updatedActivity);
        log.info("Activity successfully created with id {}.", updatedActivity.getId());
        return activityResponse;
    }
    public Activity createActivity2(Destination destination, ActivityRequest request) {
        if(!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Start time cannot be after End time");
        }
        if(request.getDate().isBefore(destination.getStartDate()) || request.getDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException("Date does not lie within Destination dates");
        }
        Activity activity = new Activity();
        activity.setDestination(destination);
        activity.setCategory(request.getCategory());
        activity.setCost(request.getCost());
        activity.setTitle(request.getTitle());
        activity.setDate(request.getDate());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        return activityRepository.saveAndFlush(activity);
    }
    public List<ActivityResponse> getActivities(Long destinationId) {
        log.info("Fetching all activities of Destination {}", destinationId);
        destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("Destination does not exist"));
        List<Activity> activities = activityRepository.findByDestinationId(destinationId);
        List<ActivityResponse> activityResponses = new ArrayList<>();
        for(Activity activity : activities) {
            activityResponses.add(createActivityResponse(activity));
        }
        log.info("All activities of Destination {} retrieved", destinationId);
        return activityResponses;
    }

    public List<ActivityResponse> getActivities2(Long tripId) {
        log.info("Finding all activities inside trip {}", tripId);
        tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip " + tripId + " not exists"));
        List<Activity> activities = activityRepository.findByDestinationTripId(tripId);
        List<ActivityResponse> activityResponses = new ArrayList<>();
        for(Activity activity : activities) {
            activityResponses.add(createActivityResponse(activity));
        }
        log.info("All activities of trip {} retrieved", tripId);
        return activityResponses;
    }

    public ActivityResponse getActivityById(Long activityId) {
        log.info("Fetching activity by ID: " + activityId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No activity with this id exists"));

        ActivityResponse activityResponse = createActivityResponse(activity);
        log.info("Found activity ID: " + activity.getId());
        return activityResponse;
    }
    public ActivityResponse createActivityResponse(Activity activity) {
        ActivityResponse activityResponse = new ActivityResponse();
        activityResponse.setId(activity.getId());
        activityResponse.setStartTime(activity.getStartTime());
        activityResponse.setEndTime(activity.getEndTime());
        activityResponse.setTitle(activity.getTitle());
        activityResponse.setDate(activity.getDate());
        activityResponse.setCost(activity.getCost());
        activityResponse.setCategory(activity.getCategory());
        activityResponse.setCreatedAt(activity.getCreatedAt());
        activityResponse.setUpdatedAt(activity.getUpdatedAt());
        return activityResponse;
    }
    public void deleteActivityById(Long activityId) {
        log.info("Deleting activity {}", activityId);
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> new ResourceNotFoundException("Activity " + activityId + " not exists"));
        activityRepository.delete(activity);
        log.info("Activity {} successfully deleted", activityId);
    }
    @Transactional
    public ActivityResponse updateActivityById(Long id, ActivityRequest request) {

        log.info("Updating activity {}", id);
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Activity " + id + " does not exist"
                ));

        if (request.getTitle() != null) {
            activity.setTitle(request.getTitle().trim());
        }
        if (request.getDate() != null) {
            activity.setDate(request.getDate());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getCategory() != null) {
            activity.setCategory(request.getCategory());
        }
        if (request.getCost() != null) {
            activity.setCost(request.getCost());
        }

        if (!activity.getStartTime().isBefore(activity.getEndTime())) {
            throw new BadRequestException(
                    "Activity start time must be before end time"
            );
        }

        Destination destination = activity.getDestination();

        if (activity.getDate().isBefore(destination.getStartDate())
                || activity.getDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Activity date must fall within destination dates"
            );
        }

        if (activity.getTitle().isBlank()) {
            throw new BadRequestException("Title must not be blank");
        }

        if (activity.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Cost cannot be negative");
        }

        Activity updatedActivity = activityRepository.saveAndFlush(activity);

        ActivityResponse activityResponse = createActivityResponse(updatedActivity);
        log.info("Activity {} successfully updated", id);
        return activityResponse;
    }
}