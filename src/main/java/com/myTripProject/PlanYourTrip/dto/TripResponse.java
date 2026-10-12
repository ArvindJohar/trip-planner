package com.myTripProject.PlanYourTrip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripResponse {
    private Long id;
    private String title;
    private String country;
    private LocalDate startDate;
    private LocalDate endDate;
    private int travellers;
    private String currency;
    private BigDecimal totalBudget;
    private List<DestinationResponse> destinations;
}