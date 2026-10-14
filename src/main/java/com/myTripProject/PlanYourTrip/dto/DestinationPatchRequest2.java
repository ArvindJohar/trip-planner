package com.myTripProject.PlanYourTrip.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DestinationPatchRequest2 {

    private String city;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<ActivityPatchRequest> activities;
}
