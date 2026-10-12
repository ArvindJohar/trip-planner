
package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.TripRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityService activityServiceBean;

    @InjectMocks
    private TripService tripService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void shouldReturnAllTripsWhenNoFiltersProvided() {
        Page<Trip> trips = new PageImpl<>(List.of());

        when(tripRepository.findAll(pageable)).thenReturn(trips);

        tripService.getTrips(null, null, null, pageable);

        verify(tripRepository).findAll(pageable);
    }

    @Test
    void shouldFilterTripsByTitle() {
        when(tripRepository.findByTitleContainingIgnoreCase(
                "Japan", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        tripService.getTrips("Japan", null, null, pageable);

        verify(tripRepository)
                .findByTitleContainingIgnoreCase("Japan", pageable);
    }

    @Test
    void shouldFilterTripsByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 11, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);

        when(tripRepository.findByStartDateBetween(
                fromDate, toDate, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        tripService.getTrips(null, fromDate, toDate, pageable);

        verify(tripRepository)
                .findByStartDateBetween(fromDate, toDate, pageable);
    }

    @Test
    void shouldFilterTripsByTitleAndDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 11, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);

        when(tripRepository
                .findByTitleContainingIgnoreCaseAndStartDateBetween(
                        "Japan", fromDate, toDate, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        tripService.getTrips(
                "Japan", fromDate, toDate, pageable);

        verify(tripRepository)
                .findByTitleContainingIgnoreCaseAndStartDateBetween(
                        "Japan", fromDate, toDate, pageable);
    }

    @Test
    void shouldRejectWhenOnlyOneDateIsProvided() {
        LocalDate fromDate = LocalDate.of(2026, 11, 1);

        assertThrows(
                BadRequestException.class,
                () -> tripService.getTrips(
                        null, fromDate, null, pageable)
        );

        verifyNoInteractions(tripRepository);
    }

    @Test
    void shouldRejectWhenFromDateIsAfterToDate() {
        LocalDate fromDate = LocalDate.of(2026, 12, 31);
        LocalDate toDate = LocalDate.of(2026, 11, 1);

        assertThrows(
                BadRequestException.class,
                () -> tripService.getTrips(
                        null, fromDate, toDate, pageable)
        );

        verifyNoInteractions(tripRepository);
    }
}
