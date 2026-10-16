package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.ActivityPatchRequest2;
import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.entity.Activity;
import com.myTripProject.PlanYourTrip.entity.Destination;
import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.exception.ResourceNotFoundException;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;
    private final CurrentUserService currentUserService;

    public ActivityService(
            ActivityRepository activityRepository,
            DestinationRepository destinationRepository,
            TripRepository tripRepository,
            CurrentUserService currentUserService
    ) {
        this.activityRepository = activityRepository;
        this.destinationRepository = destinationRepository;
        this.tripRepository = tripRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ActivityResponse createActivity(
            Long destinationId,
            LocalDate date,
            ActivityRequest request
    ) {
        log.info("Creating new activity...");

        Destination destination = getOwnedDestinationById(destinationId);

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException(
                    "Activity end time must be after start time"
            );
        }

        if (date.isBefore(destination.getStartDate())
                || date.isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Activity date must fall within destination dates"
            );
        }

        Activity activity = new Activity();

        activity.setDestination(destination);
        activity.setCategory(request.getCategory());
        activity.setCost(request.getCost());
        activity.setTitle(request.getTitle());
        activity.setDate(date);
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());

        // Append the new activity after existing activities.
        Integer maxSortOrder =
                activityRepository.findMaxSortOrderByDestinationId(destinationId);

        activity.setSortOrder(
                maxSortOrder == null ? 1 : maxSortOrder + 1
        );

        Activity savedActivity = activityRepository.save(activity);

        touchDestination(destinationId);
        touchTrip(destination.getTrip().getId());

        log.info(
                "Activity successfully created with id: {}",
                savedActivity.getId()
        );

        return createActivityResponse(savedActivity);
    }

    public List<ActivityResponse> getActivities(Long destinationId) {
        log.info("Fetching all activities of Destination {}", destinationId);

        // Verify that the destination belongs to the current user's trip.
        getOwnedDestinationById(destinationId);

        List<Activity> activities =
                activityRepository
                        .findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
                                destinationId
                        );

        List<ActivityResponse> activityResponses = new ArrayList<>();

        for (Activity activity : activities) {
            activityResponses.add(createActivityResponse(activity));
        }

        log.info("All activities of Destination {} retrieved", destinationId);

        return activityResponses;
    }

    public List<ActivityResponse> getActivities2(Long tripId) {
        log.info("Finding all activities inside trip {}", tripId);

        // Only allow access to activities inside the current user's trip.
        currentUserService.getOwnedTripById(tripId);

        List<Activity> activities =
                activityRepository
                        .findByDestinationTripIdOrderByDateAscStartTimeAscSortOrderAsc(
                                tripId
                        );

        List<ActivityResponse> activityResponses = new ArrayList<>();

        for (Activity activity : activities) {
            activityResponses.add(createActivityResponse(activity));
        }

        log.info("All activities of trip {} retrieved", tripId);

        return activityResponses;
    }

    public ActivityResponse getActivityById(Long activityId) {
        log.info("Fetching activity by ID: {}", activityId);

        Activity activity = getOwnedActivityById(activityId);

        log.info("Found activity ID: {}", activity.getId());

        return createActivityResponse(activity);
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
        activityResponse.setSortOrder(activity.getSortOrder());

        return activityResponse;
    }

    @Transactional
    public void deleteActivityById(Long activityId) {
        log.info("Deleting activity {}", activityId);

        // Verify ownership before deleting.
        Activity activity = getOwnedActivityById(activityId);

        Destination destination = activity.getDestination();

        activityRepository.delete(activity);

        touchDestination(destination.getId());
        touchTrip(destination.getTrip().getId());

        log.info("Activity {} successfully deleted", activityId);
    }

    @Transactional
    public ActivityResponse updateActivityById(
            Long id,
            ActivityPatchRequest2 request
    ) {
        log.info("Updating activity {}", id);

        // Verify ownership before modifying the activity.
        Activity activity = getOwnedActivityById(id);

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

        touchDestination(destination.getId());
        touchTrip(destination.getTrip().getId());

        log.info("Activity {} successfully updated", id);

        return createActivityResponse(updatedActivity);
    }

    private Activity getOwnedActivityById(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Activity not found"
                ));

        // An activity belongs to a destination, which belongs to a trip.
        // Verify that the current user owns that trip.
        currentUserService.getOwnedTripById(
                activity.getDestination().getTrip().getId()
        );

        return activity;
    }

    private Destination getOwnedDestinationById(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination not found"
                ));

        currentUserService.getOwnedTripById(
                destination.getTrip().getId()
        );

        return destination;
    }

    private void touchTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        trip.setUpdatedAt(java.time.LocalDateTime.now());
        tripRepository.saveAndFlush(trip);
    }

    private void touchDestination(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination not found"
                ));

        destination.setUpdatedAt(java.time.LocalDateTime.now());
        destinationRepository.saveAndFlush(destination);
    }
}
