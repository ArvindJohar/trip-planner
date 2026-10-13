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
public class TripPatchRequest {
    private String title;
    private String country;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer travellers;
    private String currency;
    private BigDecimal totalBudget;
    private List<DestinationPatchRequest2> destinations;
}
