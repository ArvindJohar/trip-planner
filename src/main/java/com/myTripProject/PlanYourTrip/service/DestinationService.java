package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.*;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DestinationService {
    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityServiceBean;
    DestinationService(DestinationRepository destinationRepository, TripRepository tripRepository, ActivityRepository activityRepository, ActivityService activityServiceBean) {
        this.destinationRepository = destinationRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.activityServiceBean = activityServiceBean;
    }
    //abc
    @Transactional
    public DestinationResponse createDestination(
            Long tripId,
            DestinationRequest request) {

        log.info("Creating new destination in Trip {}...", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        if(request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after End date");
        }

        if(request.getStartDate().isBefore(trip.getStartDate()) || request.getEndDate().isAfter(trip.getEndDate())) {
            throw new BadRequestException("Destination dates must lie within Trip dates");
        }

        Destination destination = new Destination();

        destination.setTrip(trip);
        destination.setCity(request.getCity());
        destination.setStartDate(request.getStartDate());
        destination.setEndDate(request.getEndDate());

        Destination updatedDestination = destinationRepository.save(destination);

        if (request.getActivities() != null) {
            for (ActivityRequest activityRequest : request.getActivities()) {
                activityServiceBean.createActivity(updatedDestination.getId(), activityRequest);
            }
        }

        DestinationResponse response = createDestinationResponse(updatedDestination);
        log.info("Destination successfully created with id: {}", updatedDestination.getId());
        return response;
    }
    public List<DestinationResponse> getDestinations(Long tripId) {
        log.info("Retrieving all destinations inside Trip {}", tripId);
        List<Destination> destinations = destinationRepository.findByTripId(tripId);
        List<DestinationResponse> destinationResponses = new ArrayList<>();
        for(Destination destination : destinations) {
            destinationResponses.add(createDestinationResponse(destination));
        }
        log.info("All destinations of Trip: {} retrieved", tripId);
        return destinationResponses;
    }

    public DestinationResponse createDestinationResponse(Destination destination) {
        DestinationResponse destinationResponse = new DestinationResponse();
        destinationResponse.setId(destination.getId());
        destinationResponse.setCity(destination.getCity());
        destinationResponse.setStartDate(destination.getStartDate());
        destinationResponse.setEndDate(destination.getEndDate());
        destinationResponse.setCreatedAt(destination.getCreatedAt());
        destinationResponse.setUpdatedAt(destination.getUpdatedAt());
        List<Activity> activities = activityRepository.findByDestinationId(destination.getId());
        List<ActivityResponse> activityResponses = new ArrayList<>();
        for(Activity activity : activities) {
            ActivityResponse activityResponse = activityServiceBean.createActivityResponse(activity);
            activityResponses.add(activityResponse);
        }
        destinationResponse.setActivities(activityResponses);
        return destinationResponse;
    }

    public DestinationResponse getDestinationById(Long destinationId) {
        log.info("Retrieving destination {}...", destinationId);
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No destination with the requested id exists"));
        DestinationResponse destinationResponse =  createDestinationResponse(destination);
        log.info("Destination {} successfully retrieved.", destinationId);
        return destinationResponse;
    }
    public void deleteDestinationById(Long destinationId) {
        log.info("Removing destination {}", destinationId);
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No such destination exists"));
        destinationRepository.delete(destination);
        log.info("Destination {} successfully removed.", destinationId);
    }
    @Transactional
    public DestinationResponse updateDestinationById(
            Long destinationId, DestinationPatchRequest2 request) {

        log.info("Updating destination: {}", destinationId);
        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> {
                    log.warn("Destination {} does not exist.", destinationId);
                    return new ResourceNotFoundException("Destination not exists");
                });


        // 1. Apply Destination updates
        if (request.getCity() != null) {
            destination.setCity(request.getCity().trim());
        }

        if (request.getStartDate() != null) {
            destination.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            destination.setEndDate(request.getEndDate());
        }

        // 2. Apply Activity updates
        if (request.getActivities() != null) {

            for (ActivityPatchRequest activityPatchRequest : request.getActivities()) {

                if (activityPatchRequest.getActivityId() == null) {
                    throw new BadRequestException(
                            "Id not provided for an activity");
                }

                ActivityPatchRequest2 activityPatchRequest2 = new ActivityPatchRequest2();
                activityPatchRequest2.setCategory(activityPatchRequest.getCategory());
                activityPatchRequest2.setCost(activityPatchRequest.getCost());
                activityPatchRequest2.setTitle(activityPatchRequest.getTitle());
                activityPatchRequest2.setDate(activityPatchRequest.getDate());
                activityPatchRequest2.setStartTime(activityPatchRequest.getStartTime());
                activityPatchRequest2.setEndTime(activityPatchRequest.getEndTime());

                activityServiceBean.updateActivityById(activityPatchRequest.getActivityId(), activityPatchRequest2);
            }
        }

        // 3. Validate Destination's own fields
        if (destination.getCity().isBlank()) {
            throw new BadRequestException("City must not be blank");
        }

        if (destination.getStartDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Start date cannot be after end date");
        }

        // 4. Validate Destination dates against its Trip
        Trip trip = destination.getTrip();

        if (destination.getStartDate().isBefore(trip.getStartDate())
                || destination.getEndDate().isAfter(trip.getEndDate())) {
            throw new BadRequestException(
                    "Destination dates must lie within trip dates");
        }

        // 5. Validate every Activity, including those not in the request
        List<Activity> allActivities =
                activityRepository.findByDestinationId(destinationId);

        for (Activity activity : allActivities) {

            if (activity.getDate().isBefore(destination.getStartDate())
                    || activity.getDate().isAfter(destination.getEndDate())) {
                throw new BadRequestException(
                        "Activity date must lie within destination dates");
            }
        }

        // 6. Save Destination and return response
        Destination updatedDestination =
                destinationRepository.saveAndFlush(destination);

        DestinationResponse destinationResponse = createDestinationResponse(updatedDestination);
        log.info("Destination {} updated successfully.", destinationId);
        return destinationResponse;
    }
}
