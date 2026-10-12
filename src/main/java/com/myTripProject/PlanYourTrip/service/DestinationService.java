package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.ActivityRequest;
import com.myTripProject.PlanYourTrip.dto.ActivityResponse;
import com.myTripProject.PlanYourTrip.dto.DestinationRequest;
import com.myTripProject.PlanYourTrip.dto.DestinationResponse;
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
}
