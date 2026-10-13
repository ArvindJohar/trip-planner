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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    @Transactional
    public DestinationResponse createDestination(
            Long tripId,
            DestinationRequest request) {

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

        destinationRepository.save(destination);

        List<ActivityResponse> activityResponses = new ArrayList<>();

        if (request.getActivities() != null) {
            for (ActivityRequest activityRequest : request.getActivities()) {

                Activity activity = activityServiceBean.createActivity2(destination, activityRequest);
                activityRepository.save(activity);

                ActivityResponse activityResponse = activityServiceBean.createActivityResponse(activity);

                activityResponses.add(activityResponse);
            }
        }

        DestinationResponse response = new DestinationResponse();

        response.setId(destination.getId());
        response.setCity(destination.getCity());
        response.setStartDate(destination.getStartDate());
        response.setEndDate(destination.getEndDate());
        response.setActivities(activityResponses);

        return response;
    }
    public List<DestinationResponse> getDestinations(Long tripId) {
        List<Destination> destinations = destinationRepository.findByTripId(tripId);
        List<DestinationResponse> destinationResponses = new ArrayList<>();
        for(Destination destination : destinations) {
            destinationResponses.add(createDestinationResponse(destination));
        }
        return destinationResponses;
    }

    public DestinationResponse createDestinationResponse(Destination destination) {
        DestinationResponse destinationResponse = new DestinationResponse();
        destinationResponse.setId(destination.getId());
        destinationResponse.setCity(destination.getCity());
        destinationResponse.setStartDate(destination.getStartDate());
        destinationResponse.setEndDate(destination.getEndDate());
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
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No destination with the requested id exists"));
        return createDestinationResponse(destination);
    }
    public void deleteDestinationById(Long destinationId) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("No such destination exists"));
        destinationRepository.delete(destination);
    }
    @Transactional
    public DestinationResponse updateDestinationById(Long destinationId, DestinationPatchRequest request) {
        Destination destination = destinationRepository.findById(destinationId).orElseThrow(() -> new ResourceNotFoundException("Destination not exists"));
        if(request.getCity() != null) destination.setCity(request.getCity());
        if(request.getStartDate() != null) destination.setStartDate(request.getStartDate());
        if(request.getEndDate() != null) destination.setEndDate(request.getEndDate());
        if (destination.getStartDate().isAfter(destination.getEndDate())) {
            throw new BadRequestException(
                    "Start date cannot be after end date"
            );
        }
        Trip trip = destination.getTrip();

        if (destination.getStartDate().isBefore(trip.getStartDate())
                || destination.getEndDate().isAfter(trip.getEndDate())) {
            throw new BadRequestException(
                    "Destination dates must lie within trip dates"
            );
        }

        if(request.getActivities() != null) {
            for(ActivityPatchRequest activityPatchRequest : request.getActivities()) {
                if(activityPatchRequest.getActivityId() == null) throw new BadRequestException("Id not provided for an activity");
                Activity activity = activityRepository.findById(activityPatchRequest.getActivityId()).orElseThrow(() -> new BadRequestException("Incorrect data found"));
                if(!activity.getDestination().equals(destination)) throw new BadRequestException("Activity does not belong to this destination.");
                if (activityPatchRequest.getTitle() != null && activityPatchRequest.getTitle().isBlank()) {
                    throw new BadRequestException("Title must not be blank");
                }
                if (activityPatchRequest.getCost() != null
                        && activityPatchRequest.getCost().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("Cost cannot be negative");
                }
                if(activityPatchRequest.getStartTime() != null) activity.setStartTime(activityPatchRequest.getStartTime());
                if(activityPatchRequest.getEndTime() != null) activity.setEndTime(activityPatchRequest.getEndTime());
                if(activityPatchRequest.getCost() != null) activity.setCost(activityPatchRequest.getCost());
                if(activityPatchRequest.getTitle() != null) activity.setTitle(activityPatchRequest.getTitle());
                if(activityPatchRequest.getCategory() != null) activity.setCategory(activityPatchRequest.getCategory());
                if(activityPatchRequest.getDate() != null) activity.setDate(activityPatchRequest.getDate());
                if(!activity.getStartTime().isBefore(activity.getEndTime())) throw new BadRequestException("Start time must be before End time");

                if (activity.getDate().isBefore(destination.getStartDate())
                        || activity.getDate().isAfter(destination.getEndDate())) {
                    throw new BadRequestException(
                            "Activity date must lie within destination dates"
                    );
                }
                activityRepository.save(activity);
            }
        }
        Destination updatedDestination = destinationRepository.save(destination);
        return createDestinationResponse(updatedDestination);
    }
}
