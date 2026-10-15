package com.myTripProject.PlanYourTrip.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DestinationPatchRequest {

    @Positive(message = "Destination id must be positive")
    private Long destinationId;

    private String country;

    private String city;

    @Positive(message = "Destination days must be positive")
    private Integer days;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    private List<ActivityPatchRequest> activities;
}