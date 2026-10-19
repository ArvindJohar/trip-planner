package com.myTripProject.PlanYourTrip.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DestinationResponse {
    private Long id;
    private String country;
    private String city;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer sortOrder;
    private Integer days;
    private List<ActivityResponse> activities;
}
