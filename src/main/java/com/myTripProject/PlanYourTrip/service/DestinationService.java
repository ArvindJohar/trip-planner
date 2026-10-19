
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
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityServiceBean;
    private final CurrentUserService currentUserService;
    private final PlaceImageService placeImageService;

    public DestinationService(
            DestinationRepository destinationRepository,
            TripRepository tripRepository,
            ActivityRepository activityRepository,
            ActivityService activityServiceBean,
            CurrentUserService currentUserService, PlaceImageService placeImageService
    ) {
        this.destinationRepository = destinationRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.activityServiceBean = activityServiceBean;
        this.currentUserService = currentUserService;
        this.placeImageService = placeImageService;
    }

    @Transactional
    public DestinationResponse createDestination(
            Long tripId,
            DestinationRequest request
    ) {

        log.info("Creating new destination in Trip {}...", tripId);

        Trip trip = currentUserService.getOwnedTripById(tripId);

        if (trip.getStartDate() == null || trip.getEndDate() == null) {
            throw new BadRequestException(
                    "Trip start date and end date are required"
            );
        }

        if (trip.getEndDate().isBefore(trip.getStartDate())) {
            throw new BadRequestException(
                    "Trip end date cannot be before start date"
            );
        }

        List<Destination> existingDestinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        tripId
                );

        Destination destination = new Destination();
        destination.setTrip(trip);
        destination.setCountry(request.getCountry());
        destination.setCity(request.getCity());

        // Temporary valid values; the helper assigns the final allocation.
        destination.setDays(1);
        destination.setSortOrder(existingDestinations.size() + 1);
        destination.setStartDate(trip.getStartDate());
        destination.setEndDate(trip.getStartDate());

        destinationRepository.save(destination);

        rebalanceDestinations(tripId);
        touchTrip(tripId);

        Destination savedDestination = destinationRepository.findById(
                destination.getId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Destination not found after creation"
        ));

        log.info(
                "Destination successfully created with id: {}",
                savedDestination.getId()
        );

        return createDestinationResponse(savedDestination);
    }

    public List<DestinationResponse> getDestinations(Long tripId) {

        log.info("Retrieving all destinations inside Trip {}", tripId);

        // Ensure the trip belongs to the authenticated user.
        currentUserService.getOwnedTripById(tripId);

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        tripId
                );

        List<DestinationResponse> destinationResponses =
                new ArrayList<>();

        for (Destination destination : destinations) {
            destinationResponses.add(
                    createDestinationResponse(destination)
            );
        }

        log.info("All destinations of Trip: {} retrieved", tripId);

        return destinationResponses;
    }

    public DestinationResponse createDestinationResponse(Destination destination) {

        DestinationResponse destinationResponse = new DestinationResponse();

        destinationResponse.setId(destination.getId());
        destinationResponse.setCountry(destination.getCountry());
        destinationResponse.setCity(destination.getCity());
        destinationResponse.setImageUrl(
                placeImageService.getImageUrl(
                        destination.getCity(),
                        destination.getCountry()
                )
        );
        destinationResponse.setSortOrder(destination.getSortOrder());
        destinationResponse.setDays(destination.getDays());
        destinationResponse.setStartDate(destination.getStartDate());
        destinationResponse.setEndDate(destination.getEndDate());
        destinationResponse.setCreatedAt(destination.getCreatedAt());
        destinationResponse.setUpdatedAt(destination.getUpdatedAt());

        List<Activity> activities =
                activityRepository
                        .findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
                                destination.getId()
                        );

        List<ActivityResponse> activityResponses = new ArrayList<>();

        for (Activity activity : activities) {
            ActivityResponse activityResponse =
                    activityServiceBean.createActivityResponse(activity);

            activityResponses.add(activityResponse);
        }

        destinationResponse.setActivities(activityResponses);

        return destinationResponse;
    }

    public DestinationResponse getDestinationById(Long destinationId) {

        log.info("Retrieving destination {}...", destinationId);

        Destination destination =
                getOwnedDestinationById(destinationId);

        DestinationResponse destinationResponse =
                createDestinationResponse(destination);

        log.info("Destination {} successfully retrieved.", destinationId);

        return destinationResponse;
    }

    @Transactional
    public void deleteDestinationById(Long destinationId) {

        log.info("Removing destination {}", destinationId);

        Destination destination =
                getOwnedDestinationById(destinationId);

        Long tripId = destination.getTrip().getId();

        destinationRepository.delete(destination);

        rebalanceDestinations(tripId);
        touchTrip(tripId);

        log.info("Destination {} successfully removed.", destinationId);
    }

    private void rebalanceDestinations(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        tripId
                );

        // Nothing to redistribute if no destinations remain.
        if (destinations.isEmpty()) {
            return;
        }

        long totalTripDays =
                java.time.temporal.ChronoUnit.DAYS.between(
                        trip.getStartDate(),
                        trip.getEndDate()
                ) + 1;

        int destinationCount = destinations.size();

        int baseDays = (int) (totalTripDays / destinationCount);
        int extraDays = (int) (totalTripDays % destinationCount);

        java.time.LocalDate currentStartDate = trip.getStartDate();

        for (int i = 0; i < destinationCount; i++) {

            Destination current = destinations.get(i);

            int allocatedDays = baseDays;

            // Distribute any remaining days among the first destinations.
            if (i < extraDays) {
                allocatedDays++;
            }

            java.time.LocalDate currentEndDate =
                    currentStartDate.plusDays(allocatedDays - 1L);

            current.setSortOrder(i + 1);
            current.setDays(allocatedDays);
            current.setStartDate(currentStartDate);
            current.setEndDate(currentEndDate);

            currentStartDate = currentEndDate.plusDays(1);
        }

        destinationRepository.saveAll(destinations);
    }

    @Transactional
    public DestinationResponse updateDestinationById(
            Long destinationId,
            DestinationPatchRequest2 request
    ) {

        log.info("Updating destination: {}", destinationId);

        Destination destination =
                getOwnedDestinationById(destinationId);

        // 1. Apply destination updates
        if (request.getCountry() != null) {
            destination.setCountry(request.getCountry().trim());
        }

        if (request.getCity() != null) {
            destination.setCity(request.getCity().trim());
        }

        if (request.getDays() != null) {
            if (request.getDays() <= 0) {
                throw new BadRequestException(
                        "Destination days must be positive"
                );
            }

            destination.setDays(request.getDays());
        }

        if (request.getSortOrder() != null) {
            if (request.getSortOrder() <= 0) {
                throw new BadRequestException(
                        "Destination order must be positive"
                );
            }

            reorderDestination(destination, request.getSortOrder());
        }

        if (destination.getCountry() == null
                || destination.getCountry().isBlank()) {
            throw new BadRequestException("Country must not be blank");
        }

        if (destination.getCity() == null
                || destination.getCity().isBlank()) {
            throw new BadRequestException("City must not be blank");
        }

        // 2. Apply activity updates
        if (request.getActivities() != null) {

            for (ActivityPatchRequest activityRequest :
                    request.getActivities()) {

                if (activityRequest.getActivityId() == null) {
                    throw new BadRequestException(
                            "Id not provided for an activity"
                    );
                }

                Activity activity = activityRepository
                        .findById(activityRequest.getActivityId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Activity not exists"
                        ));

                if (!activity.getDestination().getId()
                        .equals(destinationId)) {
                    throw new BadRequestException(
                            "Activity does not belong to this destination"
                    );
                }

                ActivityPatchRequest2 activityRequest2 =
                        new ActivityPatchRequest2();

                activityRequest2.setCategory(
                        activityRequest.getCategory()
                );
                activityRequest2.setCost(activityRequest.getCost());
                activityRequest2.setTitle(activityRequest.getTitle());
                activityRequest2.setDate(activityRequest.getDate());
                activityRequest2.setStartTime(
                        activityRequest.getStartTime()
                );
                activityRequest2.setEndTime(
                        activityRequest.getEndTime()
                );

                activityServiceBean.updateActivityById(
                        activityRequest.getActivityId(),
                        activityRequest2
                );
            }
        }

        Trip trip = destination.getTrip();

        // 3. Recalculate destination dates from their order and allocated days
        recalculateDestinationDates(trip.getId());

        // 4. Validate activities against the recalculated destination dates
        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        trip.getId()
                );

        for (Destination current : destinations) {

            List<Activity> activities =
                    activityRepository
                            .findByDestinationIdOrderByDateAscStartTimeAscSortOrderAsc(
                                    current.getId()
                            );

            for (Activity activity : activities) {
                if (activity.getDate().isBefore(current.getStartDate())
                        || activity.getDate().isAfter(current.getEndDate())) {
                    throw new BadRequestException(
                            "Activity date must lie within destination dates"
                    );
                }
            }
        }

        // 5. Save and return the updated destination
        Destination updatedDestination =
                destinationRepository.saveAndFlush(destination);

        touchTrip(trip.getId());

        log.info("Destination {} updated successfully.", destinationId);

        return createDestinationResponse(updatedDestination);
    }

    private void reorderDestination(
            Destination destination,
            Integer requestedOrder
    ) {

        Trip trip = destination.getTrip();

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        trip.getId()
                );

        destinations.sort(
                Comparator.comparing(Destination::getSortOrder)
                        .thenComparing(Destination::getId)
        );

        int oldIndex = destinations.indexOf(destination);

        if (oldIndex == -1) {
            throw new ResourceNotFoundException("Destination not found");
        }

        int newIndex = Math.min(
                requestedOrder - 1,
                destinations.size() - 1
        );

        if (oldIndex == newIndex) {
            return;
        }

        destinations.remove(oldIndex);
        destinations.add(newIndex, destination);

        for (int i = 0; i < destinations.size(); i++) {
            destinations.get(i).setSortOrder(i + 1);
        }

        destinationRepository.saveAll(destinations);
    }

    private void recalculateDestinationDates(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        List<Destination> destinations =
                destinationRepository.findByTripIdOrderBySortOrderAscIdAsc(
                        tripId
                );

        destinations.sort(
                Comparator.comparing(Destination::getSortOrder)
                        .thenComparing(Destination::getId)
        );

        long totalTripDays =
                java.time.temporal.ChronoUnit.DAYS.between(
                        trip.getStartDate(),
                        trip.getEndDate()
                ) + 1;

        long allocatedDays = destinations.stream()
                .mapToLong(Destination::getDays)
                .sum();

        if (allocatedDays > totalTripDays) {
            throw new BadRequestException(
                    "Destination days cannot exceed the total trip duration of "
                            + totalTripDays + " days"
            );
        }

        java.time.LocalDate currentStartDate = trip.getStartDate();

        for (int i = 0; i < destinations.size(); i++) {

            Destination current = destinations.get(i);

            if (current.getDays() == null || current.getDays() <= 0) {
                throw new BadRequestException(
                        "Destination days must be positive"
                );
            }

            // Ensure destination order remains sequential.
            current.setSortOrder(i + 1);
            current.setStartDate(currentStartDate);

            java.time.LocalDate currentEndDate =
                    currentStartDate.plusDays(current.getDays() - 1L);

            current.setEndDate(currentEndDate);

            currentStartDate = currentEndDate.plusDays(1);
        }

        destinationRepository.saveAll(destinations);
    }

    private void touchTrip(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));

        trip.setUpdatedAt(java.time.LocalDateTime.now());
        tripRepository.saveAndFlush(trip);
    }

    private Destination getOwnedDestinationById(Long destinationId) {

        Destination destination = destinationRepository
                .findById(destinationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination not found"
                ));

        // Verifies that the destination's parent trip belongs to this user.
        currentUserService.getOwnedTripById(
                destination.getTrip().getId()
        );

        return destination;
    }
}
