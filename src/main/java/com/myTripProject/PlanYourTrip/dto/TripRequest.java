package com.myTripProject.PlanYourTrip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String country;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Min(1)
    private int travellers;

    @NotBlank
    private String currency;

    @NotNull
    @Positive
    private BigDecimal totalBudget;

    @Valid
    private List<DestinationRequest> destinations;
}