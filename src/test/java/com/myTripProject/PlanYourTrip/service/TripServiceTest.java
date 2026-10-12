package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.dto.TripRequest;
import com.myTripProject.PlanYourTrip.dto.TripResponse;
import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import com.myTripProject.PlanYourTrip.repository.DestinationRepository;
import com.myTripProject.PlanYourTrip.repository.ActivityRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

    @Test
    void createTrip_success() {
        TripRequest request = new TripRequest();
        request.setTitle("Japan Trip");
        request.setCountry("Japan");
        request.setStartDate(LocalDate.of(2027, 5, 1));
        request.setEndDate(LocalDate.of(2027, 5, 10));
        request.setTravellers(2);
        request.setCurrency("INR");
        request.setTotalBudget(new BigDecimal("100000"));
        request.setDestinations(List.of());

        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.createTrip(request);

        assertNotNull(response);
        verify(tripRepository).save(any(Trip.class));
    }
}