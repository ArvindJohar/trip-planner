package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.dto.ActivityPatchRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DestinationPatchRequest2 {

    private String country;

    private String city;

    private Integer days;

    private Integer sortOrder;

    private List<ActivityPatchRequest> activities;
}