package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.entity.ActivityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
public class ActivityRequest {

    @NotBlank
    private String title;

    @NotNull
    private LocalDate date;

    @NotNull
    private ActivityCategory category;

    @NotNull
    @PositiveOrZero
    private BigDecimal cost;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
