package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.entity.ActivityCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ActivityPatchRequest {

    @NotNull
    private Long activityId;

    private Integer sortOrder;
    private String title;
    private LocalDate date;
    private ActivityCategory category;
    private BigDecimal cost;
    private LocalTime startTime;
    private LocalTime endTime;

}
