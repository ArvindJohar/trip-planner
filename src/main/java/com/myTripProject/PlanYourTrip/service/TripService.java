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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final DestinationService destinationServiceBean;

    public TripService(TripRepository tripRepository, DestinationRepository destinationRepository, ActivityRepository activityRepository, DestinationService destinationServiceBean) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.activityRepository = activityRepository;
        this.destinationServiceBean = destinationServiceBean;
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {

        log.info("Creating a new trip...");

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
        Trip updatedTrip = tripRepository.saveAndFlush(trip);

        if (request.getDestinations() != null) {
            for (DestinationRequest destinationRequest : request.getDestinations()) {
                destinationServiceBean.createDestination(updatedTrip.getId(), destinationRequest);
            }
        }
        TripResponse tripResponse = createTripResponse(updatedTrip);
        log.info("Successfully created the trip with id: {}", updatedTrip.getId());
        return tripResponse;
    }

    public BudgetSummaryResponse getBudgetSummary(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        List<Activity> activities =
                activityRepository.findByDestinationTripIdOrderByDateAscStartTimeAscSortOrderAsc(tripId);

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

        log.info("Retrieving all trips...");
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

        Page<TripResponse> tripResponses = trips.map(this::createTripResponse);
        log.info("Successfully retrieved all trips");
        return tripResponses;
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
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderByStartDateAscIdAsc(
                        trip.getId());

        // Add trip summary fields
        response.setDestinationCount(destinations.size());

        BudgetSummaryResponse budgetSummary =
                getBudgetSummary(trip.getId());

        response.setSpent(budgetSummary.getSpent());

        List<DestinationResponse> destinationResponses = new ArrayList<>();

        for (Destination destination : destinations) {
            DestinationResponse destinationResponse =
                    destinationServiceBean.createDestinationResponse(destination);

            destinationResponses.add(destinationResponse);
        }

        response.setDestinations(destinationResponses);

        return response;
    }

    public TripResponse getTripById(Long id) {
        log.info("Retrieving trip: {}", id);
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));
        TripResponse tripResponse = createTripResponse(trip);
        log.info("Trip {} successfully retrieved", id);
        return tripResponse;
    }

    public void deleteTripById(Long id) {
        log.info("Deleting Trip: {}", id);
        Trip trip = tripRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No such trip exists"));
        tripRepository.delete(trip);
        log.info("Trip: {} successfully deleted", id);
    }

    @Transactional
    public TripResponse updateTripById(Long id, TripPatchRequest tripPatchRequest) {

        log.info("Updating trip: {}...", id);

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip not exists"));

        // 1. Apply Trip updates
        if (tripPatchRequest.getCountry() != null) {
            trip.setCountry(tripPatchRequest.getCountry());
        }

        if (tripPatchRequest.getCurrency() != null) {
            trip.setCurrency(
                    tripPatchRequest.getCurrency().trim().toUpperCase());
        }

        if (tripPatchRequest.getTitle() != null) {
            trip.setTitle(tripPatchRequest.getTitle().trim());
        }

        if (tripPatchRequest.getStartDate() != null) {
            trip.setStartDate(tripPatchRequest.getStartDate());
        }

        if (tripPatchRequest.getEndDate() != null) {
            trip.setEndDate(tripPatchRequest.getEndDate());
        }

        if (tripPatchRequest.getTotalBudget() != null) {
            trip.setTotalBudget(tripPatchRequest.getTotalBudget());
        }

        if (tripPatchRequest.getTravellers() != null) {
            trip.setTravellers(tripPatchRequest.getTravellers());
        }

        // 2. Apply Destination and Activity updates
        if (tripPatchRequest.getDestinations() != null) {

            for (DestinationPatchRequest destinationPatchRequest
                    : tripPatchRequest.getDestinations()) {

                if (destinationPatchRequest.getDestinationId() == null) {

                    // Create a new destination
                    if (destinationPatchRequest.getCountry() == null
                            || destinationPatchRequest.getCountry().isBlank()) {
                        throw new BadRequestException(
                                "Country is required for a destination"
                        );
                    }

                    if (destinationPatchRequest.getCity() == null
                            || destinationPatchRequest.getCity().isBlank()) {
                        throw new BadRequestException(
                                "City is required for a destination"
                        );
                    }

                    DestinationRequest destinationRequest =
                            new DestinationRequest();

                    destinationRequest.setCountry(
                            destinationPatchRequest.getCountry()
                    );
                    destinationRequest.setCity(
                            destinationPatchRequest.getCity()
                    );

                    // Initial days are rebalanced by createDestination().
                    destinationRequest.setDays(
                            destinationPatchRequest.getDays() != null
                                    ? destinationPatchRequest.getDays()
                                    : 1
                    );

                    destinationServiceBean.createDestination(
                            id,
                            destinationRequest
                    );

                } else {

                    // Update an existing destination
                    Destination destination = destinationRepository
                            .findById(destinationPatchRequest.getDestinationId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Destination not exists"
                            ));

                    if (!destination.getTrip().getId().equals(id)) {
                        throw new BadRequestException(
                                "Destination does not belong to this trip"
                        );
                    }

                    DestinationPatchRequest2 destinationPatchRequest2 =
                            new DestinationPatchRequest2();

                    destinationPatchRequest2.setCountry(
                            destinationPatchRequest.getCountry()
                    );
                    destinationPatchRequest2.setCity(
                            destinationPatchRequest.getCity()
                    );
                    destinationPatchRequest2.setDays(
                            destinationPatchRequest.getDays()
                    );
                    destinationPatchRequest2.setSortOrder(
                            destinationPatchRequest.getSortOrder()
                    );
                    destinationPatchRequest2.setActivities(
                            destinationPatchRequest.getActivities()
                    );

                    destinationServiceBean.updateDestinationById(
                            destinationPatchRequest.getDestinationId(),
                            destinationPatchRequest2
                    );
                }
            }
        }

        // 3. Validate Trip's own fields
        if (trip.getTitle().isBlank()) {
            throw new BadRequestException("Title field cannot be empty");
        }


        if (!trip.getCurrency().matches("[A-Z]{3}")) {
            throw new BadRequestException("Invalid currency");
        }

        if (trip.getEndDate().isBefore(trip.getStartDate())) {
            throw new BadRequestException("Invalid dates");
        }

        if (trip.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget must be positive.");
        }

        if (trip.getTravellers() < 1) {
            throw new BadRequestException(
                    "At least 1 traveller required.");
        }

        destinationServiceBean.refreshDestinationDates(id);

        // 4. Validate every Destination and Activity
        List<Destination> allDestinations =
                destinationRepository.findByTripIdOrderByStartDateAscIdAsc(id);

        for (Destination destination : allDestinations) {
            if (destination.getStartDate().isBefore(trip.getStartDate())
                    || destination.getEndDate().isAfter(trip.getEndDate())) {
                throw new BadRequestException(
                        "Destination dates outside the trip dates");
            }
        }

        // 5. Save Trip and return response
        Trip updatedTrip = tripRepository.saveAndFlush(trip);

        TripResponse tripResponse = createTripResponse(updatedTrip);
        log.info("Trip {} successfully updated.", id);
        return tripResponse;
    }

}