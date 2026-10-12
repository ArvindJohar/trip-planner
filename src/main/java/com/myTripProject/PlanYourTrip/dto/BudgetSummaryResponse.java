package com.myTripProject.PlanYourTrip.dto;

import com.myTripProject.PlanYourTrip.entity.ActivityCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class BudgetSummaryResponse {

    private BigDecimal total;
    private BigDecimal spent;
    private BigDecimal remaining;
    private Map<ActivityCategory, BigDecimal> spendByCategory;
}