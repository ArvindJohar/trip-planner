package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.*;
import com.myTripProject.PlanYourTrip.entity.Activity;
import com.myTripProject.PlanYourTrip.entity.ActivityCategory;
import com.myTripProject.PlanYourTrip.entity.Destination;
import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.exception.ResourceNotFoundException;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityServiceBean;

    public TripService(TripRepository tripRepository, DestinationRepository destinationRepository, ActivityRepository activityRepository, ActivityService activityServiceBean) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.activityRepository = activityRepository;
        this.activityServiceBean = activityServiceBean;
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {


        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException(
                    "Trip start date must be before or equal to trip end date"
            );
        }
        Trip trip = new Trip();

        trip.setTitle(request.getTitle());
        trip.setCountry(request.getCountry());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setTravellers(request.getTravellers());
        trip.setCurrency(request.getCurrency());
        trip.setTotalBudget(request.getTotalBudget());
        tripRepository.save(trip);

        TripResponse tripResponse = new TripResponse();
        tripResponse.setCountry(trip.getCountry());
        tripResponse.setCurrency(trip.getCurrency());
        tripResponse.setStartDate(trip.getStartDate());
        tripResponse.setEndDate(trip.getEndDate());
        tripResponse.setTitle(trip.getTitle());
        tripResponse.setId(trip.getId());
        tripResponse.setTravellers(trip.getTravellers());
        tripResponse.setTotalBudget(trip.getTotalBudget());
        List<DestinationResponse> destinationResponses = new ArrayList<>();

        if (request.getDestinations() != null) {

            for (DestinationRequest destinationRequest : request.getDestinations()) {
                if (destinationRequest.getStartDate().isBefore(trip.getStartDate())
                        || destinationRequest.getEndDate().isAfter(trip.getEndDate())) {

                    throw new BadRequestException(
                            "Destination dates must fall within trip dates"
                    );
                }
                if(destinationRequest.getStartDate().isAfter(destinationRequest.getEndDate())) {
                    throw new BadRequestException("Destination dates are invalid.");
                }
                Destination destination = new Destination();

                destination.setTrip(trip);
                destination.setCity(destinationRequest.getCity());
                destination.setStartDate(destinationRequest.getStartDate());
                destination.setEndDate(destinationRequest.getEndDate());
                destinationRepository.save(destination);

                DestinationResponse destinationResponse = new DestinationResponse();
                destinationResponse.setId(destination.getId());
                destinationResponse.setCity(destination.getCity());
                destinationResponse.setStartDate(destination.getStartDate());
                destinationResponse.setEndDate(destination.getEndDate());
                List<ActivityResponse> activityResponses = new ArrayList<>();

                if (destinationRequest.getActivities() != null) {

                    for (ActivityRequest activityRequest : destinationRequest.getActivities()) {

                        if (activityRequest.getDate().isBefore(destination.getStartDate())
                                || activityRequest.getDate().isAfter(destination.getEndDate())) {

                            throw new BadRequestException(
                                    "Activity date must fall within destination dates"
                            );
                        }

                        if(!activityRequest.getEndTime().isAfter(activityRequest.getStartTime())) {
                            throw new BadRequestException("Invalid time interval");
                        }

                        Activity activity = activityServiceBean.createActivity2(destination, activityRequest);
                        activityRepository.save(activity);

                        ActivityResponse activityResponse = activityServiceBean.createActivityResponse(activity);

                        activityResponses.add(activityResponse);
                    }
                }
                destinationResponse.setActivities(activityResponses);
                destinationResponses.add(destinationResponse);
            }
        }
        tripResponse.setDestinations(destinationResponses);
        return tripResponse;
    }

    public BudgetSummaryResponse getBudgetSummary(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        List<Activity> activities =
                activityRepository.findByDestinationTripId(tripId);

        BigDecimal spent = BigDecimal.ZERO;

        Map<ActivityCategory, BigDecimal> spendByCategory = new HashMap<>();

        for (Activity activity : activities) {
            spent = spent.add(activity.getCost());
            ActivityCategory category = activity.getCategory();
            BigDecimal categoryAmount =
                    spendByCategory.getOrDefault(category, BigDecimal.ZERO);
            spendByCategory.put(
                    category,
                    categoryAmount.add(activity.getCost())
            );
        }

        BigDecimal remaining =
                trip.getTotalBudget().subtract(spent);

        BudgetSummaryResponse response = new BudgetSummaryResponse();

        response.setTotal(trip.getTotalBudget());
        response.setSpent(spent);
        response.setRemaining(remaining);
        response.setSpendByCategory(spendByCategory);

        return response;
    }

    public Page<TripResponse> getTrips(
            String title,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        if ((fromDate == null) != (toDate == null)) {
            throw new BadRequestException(
                    "Both fromDate and toDate must be provided together");
        }

        if (fromDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "From date cannot be after to date");
        }

        Page<Trip> trips;

        if (title != null && !title.isBlank() && fromDate != null) {
            trips = tripRepository
                    .findByTitleContainingIgnoreCaseAndStartDateBetween(
                            title.trim(), fromDate, toDate, pageable);

        } else if (title != null && !title.isBlank()) {
            trips = tripRepository
                    .findByTitleContainingIgnoreCase(title.trim(), pageable);

        } else if (fromDate != null) {
            trips = tripRepository
                    .findByStartDateBetween(fromDate, toDate, pageable);

        } else {
            trips = tripRepository.findAll(pageable);
        }

        return trips.map(this::createTripResponse);
    }

    private TripResponse createTripResponse(Trip trip) {

        TripResponse response = new TripResponse();

        response.setId(trip.getId());
        response.setTitle(trip.getTitle());
        response.setCountry(trip.getCountry());
        response.setStartDate(trip.getStartDate());
        response.setEndDate(trip.getEndDate());
        response.setTravellers(trip.getTravellers());
        response.setCurrency(trip.getCurrency());
        response.setTotalBudget(trip.getTotalBudget());

        List<Destination> destinations =
                destinationRepository.findByTripId(trip.getId());

        List<DestinationResponse> destinationResponses = new ArrayList<>();

        for (Destination destination : destinations) {

            DestinationResponse destinationResponse = new DestinationResponse();

            destinationResponse.setId(destination.getId());
            destinationResponse.setCity(destination.getCity());
            destinationResponse.setStartDate(destination.getStartDate());
            destinationResponse.setEndDate(destination.getEndDate());

            List<Activity> activities =
                    activityRepository.findByDestinationId(destination.getId());

            List<ActivityResponse> activityResponses = new ArrayList<>();

            for (Activity activity : activities) {
                ActivityResponse activityResponse = activityServiceBean.createActivityResponse(activity);
                activityResponses.add(activityResponse);
            }

            destinationResponse.setActivities(activityResponses);
            destinationResponses.add(destinationResponse);
        }

        response.setDestinations(destinationResponses);

        return response;
    }

    public TripResponse getTripById(Long id) {

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        return createTripResponse(trip);
    }

    public void deleteTripById(Long id) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No such trip exists"));
        tripRepository.delete(trip);
    }

    @Transactional
    public TripResponse updateTripById(Long id, TripPatchRequest tripPatchRequest) {
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trip not exists"));

        if(tripPatchRequest.getCountry() != null) trip.setCountry(tripPatchRequest.getCountry());
        if(tripPatchRequest.getCurrency() != null) {
            String currency = tripPatchRequest.getCurrency().trim().toUpperCase();
            if(!currency.matches("[A-Z]{3}")) throw new BadRequestException("Invalid currency");
            trip.setCurrency(currency);
        }
        if(tripPatchRequest.getTitle() != null) {
            if(tripPatchRequest.getTitle().isBlank()) throw new BadRequestException("Title cannot be empty");
            trip.setTitle(tripPatchRequest.getTitle());
        }
        if(tripPatchRequest.getStartDate() != null) trip.setStartDate(tripPatchRequest.getStartDate());
        if(tripPatchRequest.getEndDate() != null) trip.setEndDate(tripPatchRequest.getEndDate());
        if(trip.getEndDate().isBefore(trip.getStartDate())) throw new BadRequestException("Invalid dates");
        if(tripPatchRequest.getTotalBudget() != null) {
            if(tripPatchRequest.getTotalBudget().compareTo(BigDecimal.ZERO) < 0) throw new BadRequestException("Budget cannot be negative.");
            trip.setTotalBudget(tripPatchRequest.getTotalBudget());
        }
        if(tripPatchRequest.getTravellers() != null) {
            if(tripPatchRequest.getTravellers() < 1) throw new BadRequestException("Atleast 1 traveller required.");
            trip.setTravellers(tripPatchRequest.getTravellers());
        }
        if(tripPatchRequest.getDestinations() != null) {
            for(DestinationPatchRequest2 destinationPatchRequest2: tripPatchRequest.getDestinations()) {
                if(destinationPatchRequest2.getDestinationId() == null) throw new BadRequestException("Destination Id not mentioned.");
                Destination destination = destinationRepository.findById(destinationPatchRequest2.getDestinationId()).orElseThrow(() -> new ResourceNotFoundException("Destination not exists"));
                if(!destination.getTrip().getId().equals(id)) throw new BadRequestException("Destination does not belong to the trip.");
                if(destinationPatchRequest2.getCity() != null) {
                    String city = destinationPatchRequest2.getCity().trim();
                    if (city.isEmpty()) throw new BadRequestException("Enter the city name.");
                    destination.setCity(city);
                }
                if(destinationPatchRequest2.getStartDate() != null) destination.setStartDate(destinationPatchRequest2.getStartDate());
                if(destinationPatchRequest2.getEndDate() != null) destination.setEndDate(destinationPatchRequest2.getEndDate());
                if(destination.getStartDate().isAfter(destination.getEndDate())) throw new BadRequestException("Invalid dates provided");
                if(destination.getStartDate().isBefore(trip.getStartDate()) || destination.getEndDate().isAfter(trip.getEndDate())) {
                    throw new BadRequestException("Destination dates outside the trip dates");
                }
                if(destinationPatchRequest2.getActivities() != null) {
                    for(ActivityPatchRequest activityPatchRequest : destinationPatchRequest2.getActivities()) {
                        if(activityPatchRequest.getActivityId() == null) throw new BadRequestException("Activity Id not provided");
                        Activity activity = activityRepository.findById(activityPatchRequest.getActivityId()).orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
                        if(!activity.getDestination().getId().equals(destinationPatchRequest2.getDestinationId())) {
                            throw new BadRequestException("Activity does not belong to Destination");
                        }
                        if(activityPatchRequest.getTitle() != null) {
                            String title = activityPatchRequest.getTitle().trim();
                            if(title.isBlank()) throw new BadRequestException("Title cannot be empty");
                            activity.setTitle(title);
                        }
                        if(activityPatchRequest.getDate() != null) {
                            LocalDate date = activityPatchRequest.getDate();
                            activity.setDate(date);
                        }
                        if(activity.getDate().isBefore(destination.getStartDate()) || activity.getDate().isAfter(destination.getEndDate())) {
                            throw new BadRequestException("Activity date not aligns with its Destination.");
                        }
                        if(activityPatchRequest.getCategory() != null) {
                            activity.setCategory(activityPatchRequest.getCategory());
                        }
                        if(activityPatchRequest.getCost() != null) {
                            BigDecimal cost = activityPatchRequest.getCost();
                            if(cost.compareTo(BigDecimal.ZERO) < 0) {
                                throw new BadRequestException("Cost must be positive or zero.");
                            }
                            activity.setCost(cost);
                        }
                        if(activityPatchRequest.getStartTime() != null) {
                            activity.setStartTime(activityPatchRequest.getStartTime());
                        }
                        if(activityPatchRequest.getEndTime() != null) {
                            activity.setEndTime(activityPatchRequest.getEndTime());
                        }
                        if(!activity.getStartTime().isBefore(activity.getEndTime())) {
                            throw new BadRequestException("Times added are invalid");
                        }
                        activityRepository.save(activity);
                    }
                }
                destinationRepository.save(destination);
            }
        }
        Trip updatedTrip = tripRepository.save(trip);
        return createTripResponse(updatedTrip);
    }
}