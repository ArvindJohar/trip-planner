
package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.entity.Trip;
import com.myTripProject.PlanYourTrip.entity.User;
import com.myTripProject.PlanYourTrip.exception.ResourceNotFoundException;
import com.myTripProject.PlanYourTrip.repository.TripRepository;
import com.myTripProject.PlanYourTrip.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    public CurrentUserService(
            UserRepository userRepository,
            TripRepository tripRepository
    ) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"
                ));
    }

    public Trip getOwnedTripById(Long tripId) {
        User user = getCurrentUser();

        return tripRepository.findById(tripId)
                .filter(trip ->
                        trip.getUser().getId().equals(user.getId())
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trip not found"
                ));
    }
}
