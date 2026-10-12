
package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.entity.Destination;
import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.exception.BadRequestException;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import com.myTripProject.PlanYourTrip.dto.DestinationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationService destinationService;

    @Test
    void shouldRejectDestinationOutsideTripDates() {
        // Arrange
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setStartDate(LocalDate.of(2026, 10, 10));
        trip.setEndDate(LocalDate.of(2026, 10, 15));

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));

        DestinationRequest request = new DestinationRequest();
        request.setCity("Tokyo");
        request.setStartDate(LocalDate.of(2026, 10, 9));
        request.setEndDate(LocalDate.of(2026, 10, 12));

        assertThrows(
                BadRequestException.class,
                () -> destinationService.createDestination(1L, request)
        );

        verify(destinationRepository, never())
                .save(any(Destination.class));
    }
}
