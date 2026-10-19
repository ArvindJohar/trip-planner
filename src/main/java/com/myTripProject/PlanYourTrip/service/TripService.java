
package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.*;
import com.myTripProject.PlanYourTrip.entity.*;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final DestinationService destinationServiceBean;
    private final CurrentUserService currentUserService;

    public TripService(
            TripRepository tripRepository,
            DestinationRepository destinationRepository,
            ActivityRepository activityRepository,
            DestinationService destinationServiceBean,
            CurrentUserService currentUserService
    ) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.activityRepository = activityRepository;
        this.destinationServiceBean = destinationServiceBean;
        this.currentUserService = currentUserService;
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
        trip.setUser(currentUserService.getCurrentUser());

        trip.setTitle(request.getTitle());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setTravellers(request.getTravellers());
        trip.setCurrency(request.getCurrency());
        trip.setTotalBudget(request.getTotalBudget());

        Trip updatedTrip = tripRepository.saveAndFlush(trip);

        if (request.getDestinations() != null) {
            for (DestinationRequest destinationRequest : request.getDestinations()) {
                destinationServiceBean.createDestination(
                        updatedTrip.getId(),
                        destinationRequest
                );
            }
        }

        TripResponse tripResponse = createTripResponse(updatedTrip);

        log.info(
                "Successfully created the trip with id: {}",
                updatedTrip.getId()
        );

        return tripResponse;
    }

    public BudgetSummaryResponse getBudgetSummary(Long tripId) {

        Trip trip = currentUserService.getOwnedTripById(tripId);

        List<Activity> activities =
                activityRepository
                        .findByDestinationTripIdOrderByDateAscStartTimeAscSortOrderAsc(
                                tripId
                        );

        BigDecimal spent = BigDecimal.ZERO;

        Map<ActivityCategory, BigDecimal> spendByCategory =
                new HashMap<>();

        for (Activity activity : activities) {
            spent = spent.add(activity.getCost());

            ActivityCategory category = activity.getCategory();

            BigDecimal categoryAmount =
                    spendByCategory.getOrDefault(
                            category,
                            BigDecimal.ZERO
                    );

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
            Pageable pageable
    ) {

        User user = currentUserService.getCurrentUser();

        Long userId = user.getId();

        log.info("Retrieving trips for user: {}", userId);

        if ((fromDate == null) != (toDate == null)) {
            throw new BadRequestException(
                    "Both fromDate and toDate must be provided together"
            );
        }

        if (fromDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "From date cannot be after to date"
            );
        }

        Page<Trip> trips;

        if (title != null && !title.isBlank() && fromDate != null) {
            trips = tripRepository
                    .findByUserIdAndTitleContainingIgnoreCaseAndStartDateBetween(
                            userId,
                            title.trim(),
                            fromDate,
                            toDate,
                            pageable
                    );

        } else if (title != null && !title.isBlank()) {
            trips = tripRepository
                    .findByUserIdAndTitleContainingIgnoreCase(
                            userId,
                            title.trim(),
                            pageable
                    );

        } else if (fromDate != null) {
            trips = tripRepository
                    .findByUserIdAndStartDateBetween(
                            userId,
                            fromDate,
                            toDate,
                            pageable
                    );

        } else {
            trips = tripRepository.findByUserId(userId, pageable);
        }

        Page<TripResponse> tripResponses =
                trips.map(this::createTripResponse);

        log.info("Successfully retrieved trips for user: {}", userId);

        return tripResponses;
    }

    private TripResponse createTripResponse(Trip trip) {

        TripResponse response = new TripResponse();

        response.setId(trip.getId());
        response.setTitle(trip.getTitle());
        response.setStartDate(trip.getStartDate());
        response.setEndDate(trip.getEndDate());
        response.setTravellers(trip.getTravellers());
        response.setCurrency(trip.getCurrency());
        response.setTotalBudget(trip.getTotalBudget());
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        trip.getId()
                );

        // Add trip summary fields
        response.setDestinationCount(destinations.size());

        BudgetSummaryResponse budgetSummary =
                getBudgetSummary(trip.getId());

        response.setSpent(budgetSummary.getSpent());

        List<DestinationResponse> destinationResponses =
                new ArrayList<>();

        for (Destination destination : destinations) {
            DestinationResponse destinationResponse =
                    destinationServiceBean.createDestinationResponse(
                            destination
                    );

            destinationResponses.add(destinationResponse);
        }

        response.setDestinations(destinationResponses);
        String coverImageUrl = destinationResponses.stream()
                .map(DestinationResponse::getImageUrl)
                .filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
                .findFirst()
                .orElse(null);

        response.setCoverImageUrl(coverImageUrl);
        return response;
    }

    public TripResponse getTripById(Long id) {

        log.info("Retrieving trip: {}", id);

        Trip trip = currentUserService.getOwnedTripById(id);

        TripResponse tripResponse = createTripResponse(trip);

        log.info("Trip {} successfully retrieved", id);

        return tripResponse;
    }

    public void deleteTripById(Long id) {

        log.info("Deleting trip: {}", id);

        Trip trip = currentUserService.getOwnedTripById(id);

        tripRepository.delete(trip);

        log.info("Trip: {} successfully deleted", id);
    }

    @Transactional(rollbackOn = Exception.class)
    public TripResponse replaceTrip(
            Long id,
            TripUpdateRequest request
    ) {

        Trip trip = currentUserService.getOwnedTripById(id);

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {
            throw new BadRequestException("Title cannot be blank.");
        }

        if (request.getCountry() == null
                || request.getCountry().isBlank()) {
            throw new BadRequestException("Country cannot be blank.");
        }

        if (request.getCurrency() == null
                || !request.getCurrency().trim()
                .toUpperCase().matches("[A-Z]{3}")) {
            throw new BadRequestException("Invalid currency.");
        }

        if (request.getStartDate() == null
                || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Invalid trip dates.");
        }

        if (request.getTravellers() == null
                || request.getTravellers() < 1) {
            throw new BadRequestException(
                    "At least 1 traveller is required."
            );
        }

        if (request.getTotalBudget() == null
                || request.getTotalBudget()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Budget must be positive."
            );
        }

        if (request.getDestinations() == null) {
            throw new BadRequestException(
                    "Destinations list is required."
            );
        }

        trip.setTitle(request.getTitle().trim());
        trip.setCurrency(request.getCurrency().trim().toUpperCase());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setTravellers(request.getTravellers());
        trip.setTotalBudget(request.getTotalBudget());

        List<Destination> existingDestinations =
                destinationRepository
                        .findByTripIdOrderBySortOrderAscIdAsc(id);

        Map<Long, Destination> destinationById =
                existingDestinations.stream()
                        .collect(Collectors.toMap(
                                Destination::getId,
                                d -> d
                        ));

        Set<Long> requestedDestinationIds = new HashSet<>();

        for (DestinationUpdateRequest d : request.getDestinations()) {

            if (d.getId() != null) {

                if (!requestedDestinationIds.add(d.getId())) {
                    throw new BadRequestException(
                            "Duplicate destination ID: " + d.getId()
                    );
                }

                if (!destinationById.containsKey(d.getId())) {
                    throw new BadRequestException(
                            "Destination does not belong to this trip: "
                                    + d.getId()
                    );
                }
            }

            if (d.getCountry() == null || d.getCountry().isBlank()
                    || d.getCity() == null || d.getCity().isBlank()) {
                throw new BadRequestException(
                        "Destination country and city are required."
                );
            }

            if (d.getDays() == null || d.getDays() <= 0) {
                throw new BadRequestException(
                        "Destination days must be positive."
                );
            }

            if (d.getActivities() == null) {
                throw new BadRequestException(
                        "Activities list is required for each destination."
                );
            }
        }

        for (Destination existing : existingDestinations) {

            if (!requestedDestinationIds.contains(existing.getId())) {

                activityRepository.deleteAll(
                        activityRepository
                                .findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
                                        existing.getId()
                                )
                );

                destinationRepository.delete(existing);
            }
        }

        destinationRepository.flush();

        List<Destination> orderedDestinations = new ArrayList<>();

        for (int i = 0; i < request.getDestinations().size(); i++) {

            DestinationUpdateRequest d =
                    request.getDestinations().get(i);

            Destination destination;

            if (d.getId() != null) {
                destination = destinationById.get(d.getId());
            } else {
                destination = new Destination();
                destination.setTrip(trip);
            }

            destination.setCountry(d.getCountry().trim());
            destination.setCity(d.getCity().trim());
            destination.setDays(d.getDays());
            destination.setSortOrder(i + 1);

            // Temporary dates. Final dates are calculated below.
            destination.setStartDate(trip.getStartDate());
            destination.setEndDate(trip.getStartDate());

            orderedDestinations.add(
                    destinationRepository.save(destination)
            );
        }

        long tripDays = ChronoUnit.DAYS.between(
                trip.getStartDate(),
                trip.getEndDate()
        ) + 1;

        long allocatedDays = orderedDestinations.stream()
                .mapToLong(Destination::getDays)
                .sum();

        if (allocatedDays != tripDays) {
            throw new BadRequestException(
                    "Destination days must total " + tripDays
                            + " days, but total " + allocatedDays + "."
            );
        }

        LocalDate currentStart = trip.getStartDate();

        for (Destination destination : orderedDestinations) {

            destination.setStartDate(currentStart);

            LocalDate currentEnd =
                    currentStart.plusDays(destination.getDays() - 1L);

            destination.setEndDate(currentEnd);
            currentStart = currentEnd.plusDays(1);
        }

        destinationRepository.saveAllAndFlush(orderedDestinations);

        for (int i = 0; i < orderedDestinations.size(); i++) {

            Destination destination = orderedDestinations.get(i);
            DestinationUpdateRequest d =
                    request.getDestinations().get(i);

            List<Activity> existingActivities =
                    activityRepository
                            .findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
                                    destination.getId()
                            );

            Map<Long, Activity> activityById =
                    existingActivities.stream()
                            .collect(Collectors.toMap(
                                    Activity::getId,
                                    a -> a
                            ));

            Set<Long> requestedActivityIds = new HashSet<>();

            for (ActivityUpdateRequest a : d.getActivities()) {

                if (a.getId() != null) {

                    if (!requestedActivityIds.add(a.getId())) {
                        throw new BadRequestException(
                                "Duplicate activity ID: " + a.getId()
                        );
                    }

                    if (!activityById.containsKey(a.getId())) {
                        throw new BadRequestException(
                                "Activity does not belong to this destination: "
                                        + a.getId()
                        );
                    }
                }

                if (a.getTitle() == null || a.getTitle().isBlank()
                        || a.getDate() == null
                        || a.getCategory() == null
                        || a.getCost() == null
                        || a.getStartTime() == null
                        || a.getEndTime() == null) {
                    throw new BadRequestException(
                            "All activity fields are required."
                    );
                }
            }

            for (Activity existing : existingActivities) {

                if (!requestedActivityIds.contains(existing.getId())) {
                    activityRepository.delete(existing);
                }
            }

            for (int j = 0; j < d.getActivities().size(); j++) {

                ActivityUpdateRequest a = d.getActivities().get(j);

                Activity activity;

                if (a.getId() != null) {
                    activity = activityById.get(a.getId());
                } else {
                    activity = new Activity();
                    activity.setDestination(destination);
                }

                activity.setTitle(a.getTitle().trim());
                activity.setDate(a.getDate());
                activity.setCategory(a.getCategory());
                activity.setCost(a.getCost());
                activity.setStartTime(a.getStartTime());
                activity.setEndTime(a.getEndTime());
                activity.setSortOrder(j + 1);

                // Validate against final destination dates.
                if (a.getDate().isBefore(destination.getStartDate())
                        || a.getDate().isAfter(destination.getEndDate())) {
                    throw new BadRequestException(
                            "Activity date must fall within destination dates."
                    );
                }

                if (!a.getEndTime().isAfter(a.getStartTime())) {
                    throw new BadRequestException(
                            "Activity end time must be after start time."
                    );
                }

                if (a.getCost().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException(
                            "Activity cost cannot be negative."
                    );
                }

                activityRepository.save(activity);
            }
        }

        tripRepository.saveAndFlush(trip);

        return createTripResponse(trip);
    }
}
