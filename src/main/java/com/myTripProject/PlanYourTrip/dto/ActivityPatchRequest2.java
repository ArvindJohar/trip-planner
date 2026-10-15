package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.entity.ActivityCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class ActivityPatchRequest2 {
    private Integer sortOrder;
    private String title;
    private LocalDate date;
    private ActivityCategory category;
    private BigDecimal cost;
    private LocalTime startTime;
    private LocalTime endTime;
}
