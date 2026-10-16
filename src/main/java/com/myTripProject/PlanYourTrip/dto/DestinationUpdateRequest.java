
package com.myTripProject.PlanYourTrip.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DestinationUpdateRequest {
    private Long id;
    private String country;
    private String city;
    private Integer days;
    private List<ActivityUpdateRequest> activities;
}
